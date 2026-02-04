package com.dmacheese.pccheck;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinReg.HKEY;
import com.sun.jna.ptr.IntByReference;

import java.io.File;

import static com.sun.jna.platform.win32.SetupApi.DIGCF_ALLCLASSES;
import static com.sun.jna.platform.win32.SetupApi.DIGCF_PRESENT;

/**
 * Service class for performing system compatibility checks.
 * Uses JNA to access Windows Registry and Device Manager APIs.
 */
public class SystemCheckService {

    private static final int SPDRP_DEVICEDESC = 0x00000000;
    private static final int SPDRP_FRIENDLYNAME = 0x0000000C;

    /**
     * Checks if Windows Defender Real-Time Protection is disabled.
     */
    public boolean checkWindowsSecurity() {
        boolean passing = false;
        try {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE,
                    "SOFTWARE\\Microsoft\\Windows Defender\\Real-Time Protection", "DisableRealtimeMonitoring")) {
                int val = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE,
                        "SOFTWARE\\Microsoft\\Windows Defender\\Real-Time Protection", "DisableRealtimeMonitoring");
                if (val == 1) {
                    passing = true;
                }
            }
        } catch (Exception e) {
            // Registry access denied or key missing - Defender is likely enabled
        }
        Utils.printResult("Windows Security (Defender OFF)", passing,
                passing ? "Disabled" : "Enabled (Please Disable)");
        return passing;
    }

    /**
     * Checks if HVCI (Hypervisor-Enforced Code Integrity) is disabled.
     */
    public boolean checkHVCI() {
        boolean passing = true;
        try {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE,
                    "SYSTEM\\CurrentControlSet\\Control\\DeviceGuard\\Scenarios\\HypervisorEnforcedCodeIntegrity",
                    "Enabled")) {
                int val = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE,
                        "SYSTEM\\CurrentControlSet\\Control\\DeviceGuard\\Scenarios\\HypervisorEnforcedCodeIntegrity",
                        "Enabled");
                if (val == 1) {
                    passing = false;
                }
            }
        } catch (Exception e) {
            // Key missing means HVCI is not configured (default off)
        }
        Utils.printResult("HVCI / Core Isolation", passing, passing ? "OFF" : "ON (Please Disable)");
        return passing;
    }

    /**
     * Checks if the Vulnerable Driver Blocklist is disabled.
     */
    public boolean checkVulnDriverBlocklist() {
        boolean passing = true;
        try {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE,
                    "SYSTEM\\CurrentControlSet\\Control\\CI\\Config", "VulnerableDriverBlocklistEnable")) {
                int val = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE,
                        "SYSTEM\\CurrentControlSet\\Control\\CI\\Config", "VulnerableDriverBlocklistEnable");
                if (val == 1) {
                    passing = false;
                }
            }
        } catch (Exception e) {
            // Key missing means blocklist is not enabled
        }
        Utils.printResult("Vulnerable Driver Blocklist", passing, passing ? "OFF" : "ON (Please Disable)");
        return passing;
    }

    /**
     * Checks if User Account Control (UAC) is disabled.
     */
    public boolean checkUAC() {
        boolean passing = false;
        try {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE,
                    "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\System", "EnableLUA")) {
                int val = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE,
                        "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\System", "EnableLUA");
                if (val == 0) {
                    passing = true;
                }
            }
        } catch (Exception e) {
            // Key missing means UAC is enabled (default)
        }
        Utils.printResult("User Account Control (UAC)", passing, passing ? "OFF" : "ON (Please Disable)");
        return passing;
    }

    /**
     * Checks if Kernel DMA Protection / Virtualization Based Security is disabled.
     */
    public boolean checkKernelDMAProtection() {
        boolean passing = true;
        try {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE,
                    "SYSTEM\\CurrentControlSet\\Control\\DeviceGuard", "EnableVirtualizationBasedSecurity")) {
                int val = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE,
                        "SYSTEM\\CurrentControlSet\\Control\\DeviceGuard", "EnableVirtualizationBasedSecurity");
                if (val == 1) {
                    passing = false;
                }
            }
        } catch (Exception e) {
            // Key missing means VBS is not configured
        }
        Utils.printResult("Kernel DMA Protection / VBS", passing,
                passing ? "Likely OFF" : "VBS Enabled (Disable IOMMU/VT-d in BIOS)");
        return passing;
    }

    /**
     * Checks if Visual C++ Redistributable (x64) is installed.
     */
    public boolean checkVCRuntimes() {
        boolean passing = false;
        try {
            String key = "SOFTWARE\\Microsoft\\VisualStudio\\14.0\\VC\\Runtimes\\x64";
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, key, "Installed")) {
                int val = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, key, "Installed");
                if (val == 1) {
                    passing = true;
                }
            }
        } catch (Exception e) {
            // Registry key not found
        }
        Utils.printResult("Visual C++ Runtimes (x64)", passing, passing ? "Installed" : "Missing");
        return passing;
    }

    /**
     * Checks if DirectX End-User Runtimes are installed.
     */
    public boolean checkDirectX() {
        boolean passing = new File("C:\\Windows\\System32\\d3dx9_43.dll").exists();
        Utils.printResult("DirectX End-User Runtimes", passing,
                passing ? "Installed (d3dx9_43.dll found)" : "Missing (d3dx9_43.dll not found)");
        return passing;
    }

    /**
     * Checks if the FTDI FT601 USB 3.0 Bridge Device driver is installed with
     * correct version.
     */
    public boolean checkFTDIBUS() {
        String targetDevice = "FTDI FT601 USB 3.0 Bridge Device";
        SetupApi setupApi = SetupApi.INSTANCE;

        WinNT.HANDLE hDevInfo = setupApi.SetupDiGetClassDevs(null, null, null, DIGCF_PRESENT | DIGCF_ALLCLASSES);

        if (hDevInfo == WinBase.INVALID_HANDLE_VALUE) {
            Utils.printResult("FTDIBUS3 Driver", false,
                    "Failed to Enumerate Devices - Error: " + Kernel32.INSTANCE.GetLastError());
            return false;
        }

        SetupApi.SP_DEVINFO_DATA deviceInfoData = new SetupApi.SP_DEVINFO_DATA();
        deviceInfoData.cbSize = deviceInfoData.size();

        for (int i = 0; setupApi.SetupDiEnumDeviceInfo(hDevInfo, i, deviceInfoData); i++) {
            String friendlyName = getDeviceProperty(hDevInfo, deviceInfoData, SPDRP_FRIENDLYNAME);
            String descName = getDeviceProperty(hDevInfo, deviceInfoData, SPDRP_DEVICEDESC);

            boolean found = (friendlyName != null && friendlyName.contains(targetDevice))
                    || (descName != null && descName.contains(targetDevice));

            if (found) {
                String version = getDriverVersion(hDevInfo, deviceInfoData);

                if (version != null) {
                    boolean versionOK = version.equals("1.3.0.8") || version.equals("1.3.0.10");
                    if (versionOK) {
                        Utils.printResult("FTDIBUS3 Driver", true, "Installed (" + version + ")");
                        setupApi.SetupDiDestroyDeviceInfoList(hDevInfo);
                        return true;
                    } else {
                        Utils.printResult("FTDIBUS3 Driver", false,
                                "Wrong Version (" + version + ", expected 1.3.0.8/10)");
                        setupApi.SetupDiDestroyDeviceInfoList(hDevInfo);
                        return false;
                    }
                } else {
                    Utils.printResult("FTDIBUS3 Driver", false, "Driver Version Not Found");
                    setupApi.SetupDiDestroyDeviceInfoList(hDevInfo);
                    return false;
                }
            }
        }

        setupApi.SetupDiDestroyDeviceInfoList(hDevInfo);
        Utils.printResult("FTDIBUS3 Driver", false, "Device Not Found");
        return false;
    }

    private String getDeviceProperty(WinNT.HANDLE hDevInfo, SetupApi.SP_DEVINFO_DATA devInfoData, int property) {
        Memory buffer = new Memory(1024);
        IntByReference requiredSize = new IntByReference();
        if (SetupApi.INSTANCE.SetupDiGetDeviceRegistryProperty(hDevInfo, devInfoData, property, null, buffer,
                (int) buffer.size(), requiredSize)) {
            return buffer.getWideString(0);
        }
        return null;
    }

    private String getDriverVersion(WinNT.HANDLE hDevInfo, SetupApi.SP_DEVINFO_DATA devInfoData) {
        HKEY hKey = SetupApi.INSTANCE.SetupDiOpenDevRegKey(hDevInfo, devInfoData, SetupApi.DICS_FLAG_GLOBAL, 0,
                SetupApi.DIREG_DRV, WinNT.KEY_READ);
        if (hKey != null && !hKey.equals(WinBase.INVALID_HANDLE_VALUE)) {
            try {
                if (Advapi32Util.registryValueExists(hKey, "", "DriverVersion")) {
                    return Advapi32Util.registryGetStringValue(hKey, "", "DriverVersion");
                }
            } finally {
                Advapi32Util.registryCloseKey(hKey);
            }
        }
        return null;
    }
}
