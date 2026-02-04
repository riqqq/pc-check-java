package com.dmacheese.pccheck;

import com.sun.jna.Memory;
import com.sun.jna.platform.win32.*;
import com.sun.jna.platform.win32.WinReg.HKEY;
import com.sun.jna.ptr.IntByReference;

import java.io.File;
import java.util.Optional;

import static com.sun.jna.platform.win32.SetupApi.DIGCF_ALLCLASSES;
import static com.sun.jna.platform.win32.SetupApi.DIGCF_PRESENT;

/**
 * Service class for performing system compatibility checks.
 * Uses JNA to access Windows Registry and Device Manager APIs.
 */
public class SystemCheckService {

    // Registry path constants
    private static final String REG_DEFENDER_REALTIME = "SOFTWARE\\Microsoft\\Windows Defender\\Real-Time Protection";
    private static final String REG_HVCI = "SYSTEM\\CurrentControlSet\\Control\\DeviceGuard\\Scenarios\\HypervisorEnforcedCodeIntegrity";
    private static final String REG_VULN_BLOCKLIST = "SYSTEM\\CurrentControlSet\\Control\\CI\\Config";
    private static final String REG_UAC = "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\System";
    private static final String REG_VBS = "SYSTEM\\CurrentControlSet\\Control\\DeviceGuard";
    private static final String REG_VC_RUNTIMES = "SOFTWARE\\Microsoft\\VisualStudio\\14.0\\VC\\Runtimes\\x64";

    // Device property constants
    private static final int SPDRP_DEVICEDESC = 0x00000000;
    private static final int SPDRP_FRIENDLYNAME = 0x0000000C;

    // Target device configuration
    private static final String TARGET_FTDI_DEVICE = "FTDI FT601 USB 3.0 Bridge Device";
    private static final String EXPECTED_DRIVER_VERSION_1 = "1.3.0.8";
    private static final String EXPECTED_DRIVER_VERSION_2 = "1.3.0.10";

    /**
     * Checks if Windows Defender Real-Time Protection is disabled.
     */
    public boolean checkWindowsSecurity() {
        boolean passing = isRegistryValueEqual(REG_DEFENDER_REALTIME, "DisableRealtimeMonitoring", 1);
        Utils.printResult("Windows Security (Defender OFF)", passing,
                passing ? "Disabled" : "Enabled (Please Disable)");
        return passing;
    }

    /**
     * Checks if HVCI (Hypervisor-Enforced Code Integrity) is disabled.
     */
    public boolean checkHVCI() {
        boolean passing = !isRegistryValueEqual(REG_HVCI, "Enabled", 1);
        Utils.printResult("HVCI / Core Isolation", passing, passing ? "OFF" : "ON (Please Disable)");
        return passing;
    }

    /**
     * Checks if the Vulnerable Driver Blocklist is disabled.
     */
    public boolean checkVulnDriverBlocklist() {
        boolean passing = !isRegistryValueEqual(REG_VULN_BLOCKLIST, "VulnerableDriverBlocklistEnable", 1);
        Utils.printResult("Vulnerable Driver Blocklist", passing, passing ? "OFF" : "ON (Please Disable)");
        return passing;
    }

    /**
     * Checks if User Account Control (UAC) is disabled.
     */
    public boolean checkUAC() {
        boolean passing = isRegistryValueEqual(REG_UAC, "EnableLUA", 0);
        Utils.printResult("User Account Control (UAC)", passing, passing ? "OFF" : "ON (Please Disable)");
        return passing;
    }

    /**
     * Checks if Kernel DMA Protection / Virtualization Based Security is disabled.
     */
    public boolean checkKernelDMAProtection() {
        boolean passing = !isRegistryValueEqual(REG_VBS, "EnableVirtualizationBasedSecurity", 1);
        Utils.printResult("Kernel DMA Protection / VBS", passing,
                passing ? "Likely OFF" : "VBS Enabled (Disable IOMMU/VT-d in BIOS)");
        return passing;
    }

    /**
     * Checks if Visual C++ Redistributable (x64) is installed.
     */
    public boolean checkVCRuntimes() {
        boolean passing = isRegistryValueEqual(REG_VC_RUNTIMES, "Installed", 1);
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
        SetupApi setupApi = SetupApi.INSTANCE;
        WinNT.HANDLE hDevInfo = setupApi.SetupDiGetClassDevs(null, null, null, DIGCF_PRESENT | DIGCF_ALLCLASSES);

        if (hDevInfo == WinBase.INVALID_HANDLE_VALUE) {
            Utils.printResult("FTDIBUS3 Driver", false,
                    "Failed to Enumerate Devices - Error: " + Kernel32.INSTANCE.GetLastError());
            return false;
        }

        try {
            return enumerateAndCheckDevices(setupApi, hDevInfo);
        } finally {
            setupApi.SetupDiDestroyDeviceInfoList(hDevInfo);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Checks if a registry DWORD value equals the expected value.
     */
    private boolean isRegistryValueEqual(String keyPath, String valueName, int expectedValue) {
        try {
            if (Advapi32Util.registryValueExists(WinReg.HKEY_LOCAL_MACHINE, keyPath, valueName)) {
                int val = Advapi32Util.registryGetIntValue(WinReg.HKEY_LOCAL_MACHINE, keyPath, valueName);
                return val == expectedValue;
            }
        } catch (Exception e) {
            // Registry access denied or key missing
        }
        return false;
    }

    /**
     * Checks if a device name contains the target device string.
     */
    private boolean containsTargetDevice(String deviceName) {
        return deviceName != null && deviceName.contains(TARGET_FTDI_DEVICE);
    }

    /**
     * Checks if the driver version is acceptable.
     */
    private boolean isAcceptableDriverVersion(String version) {
        return EXPECTED_DRIVER_VERSION_1.equals(version) || EXPECTED_DRIVER_VERSION_2.equals(version);
    }

    /**
     * Enumerates devices and checks for FTDI device with correct driver version.
     */
    private boolean enumerateAndCheckDevices(SetupApi setupApi, WinNT.HANDLE hDevInfo) {
        SetupApi.SP_DEVINFO_DATA deviceInfoData = new SetupApi.SP_DEVINFO_DATA();
        deviceInfoData.cbSize = deviceInfoData.size();

        for (int i = 0; setupApi.SetupDiEnumDeviceInfo(hDevInfo, i, deviceInfoData); i++) {
            Optional<String> friendlyName = getDeviceProperty(hDevInfo, deviceInfoData, SPDRP_FRIENDLYNAME);
            Optional<String> descName = getDeviceProperty(hDevInfo, deviceInfoData, SPDRP_DEVICEDESC);

            boolean found = friendlyName.map(this::containsTargetDevice).orElse(false)
                    || descName.map(this::containsTargetDevice).orElse(false);

            if (found) {
                return handleFoundDevice(hDevInfo, deviceInfoData);
            }
        }

        Utils.printResult("FTDIBUS3 Driver", false, "Device Not Found");
        return false;
    }

    /**
     * Handles the case when FTDI device is found - checks driver version.
     */
    private boolean handleFoundDevice(WinNT.HANDLE hDevInfo, SetupApi.SP_DEVINFO_DATA deviceInfoData) {
        Optional<String> version = getDriverVersion(hDevInfo, deviceInfoData);

        if (version.isPresent()) {
            String ver = version.get();
            if (isAcceptableDriverVersion(ver)) {
                Utils.printResult("FTDIBUS3 Driver", true, "Installed (" + ver + ")");
                return true;
            } else {
                Utils.printResult("FTDIBUS3 Driver", false,
                        "Wrong Version (" + ver + ", expected " + EXPECTED_DRIVER_VERSION_1 + "/10)");
                return false;
            }
        } else {
            Utils.printResult("FTDIBUS3 Driver", false, "Driver Version Not Found");
            return false;
        }
    }

    /**
     * Gets a device property as Optional.
     */
    private Optional<String> getDeviceProperty(WinNT.HANDLE hDevInfo, SetupApi.SP_DEVINFO_DATA devInfoData,
            int property) {
        Memory buffer = new Memory(1024);
        IntByReference requiredSize = new IntByReference();
        if (SetupApi.INSTANCE.SetupDiGetDeviceRegistryProperty(hDevInfo, devInfoData, property, null, buffer,
                (int) buffer.size(), requiredSize)) {
            return Optional.of(buffer.getWideString(0));
        }
        return Optional.empty();
    }

    /**
     * Gets driver version from device registry key as Optional.
     */
    private Optional<String> getDriverVersion(WinNT.HANDLE hDevInfo, SetupApi.SP_DEVINFO_DATA devInfoData) {
        HKEY hKey = SetupApi.INSTANCE.SetupDiOpenDevRegKey(hDevInfo, devInfoData, SetupApi.DICS_FLAG_GLOBAL, 0,
                SetupApi.DIREG_DRV, WinNT.KEY_READ);
        if (hKey != null && !hKey.equals(WinBase.INVALID_HANDLE_VALUE)) {
            try {
                if (Advapi32Util.registryValueExists(hKey, "", "DriverVersion")) {
                    return Optional.of(Advapi32Util.registryGetStringValue(hKey, "", "DriverVersion"));
                }
            } finally {
                Advapi32Util.registryCloseKey(hKey);
            }
        }
        return Optional.empty();
    }
}
