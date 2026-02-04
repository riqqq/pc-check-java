package com.dmacheese.pccheck;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Shell32;
import com.sun.jna.platform.win32.WinReg;

/**
 * Service class for applying automated fixes to system settings.
 * Requires Administrator privileges to modify registry values.
 */
public class FixService {

    /**
     * Attempts to disable Windows Defender Real-Time Protection.
     * Note: This may fail if Tamper Protection is enabled.
     */
    public boolean fixWindowsSecurity() {
        System.out.println(" -> Attempting to disable Real-Time Protection...");
        try {
            Advapi32Util.registrySetIntValue(WinReg.HKEY_LOCAL_MACHINE,
                    "SOFTWARE\\Microsoft\\Windows Defender\\Real-Time Protection", "DisableRealtimeMonitoring", 1);
            return true;
        } catch (Exception e) {
            Utils.setConsoleColor(Utils.Color.Red);
            System.out.println("    [FAILED] Could not disable Real-Time Protection.");
            System.out.println("    [!] Tamper Protection is likely ON.");
            Utils.setConsoleColor(Utils.Color.Yellow);
            System.out.println("    [ACTION] Please manually disable 'Tamper Protection' in Windows Security.");
            System.out.println("             Opening Windows Security settings...");
            Utils.setConsoleColor(Utils.Color.Default);

            Shell32.INSTANCE.ShellExecute(null, "open", "windowsdefender://threat/", null, null, 1);
            return false;
        }
    }

    /**
     * Attempts to disable HVCI (Hypervisor-Enforced Code Integrity).
     */
    public boolean fixHVCI() {
        System.out.println(" -> Attempting to disable HVCI (Core Isolation)...");
        try {
            Advapi32Util.registrySetIntValue(WinReg.HKEY_LOCAL_MACHINE,
                    "SYSTEM\\CurrentControlSet\\Control\\DeviceGuard\\Scenarios\\HypervisorEnforcedCodeIntegrity",
                    "Enabled", 0);
            return true;
        } catch (Exception e) {
            System.out.println("    [FAILED] " + e.getMessage());
            return false;
        }
    }

    /**
     * Attempts to disable the Vulnerable Driver Blocklist.
     */
    public boolean fixVulnDriverBlocklist() {
        System.out.println(" -> Attempting to disable Vulnerable Driver Blocklist...");
        try {
            Advapi32Util.registrySetIntValue(WinReg.HKEY_LOCAL_MACHINE,
                    "SYSTEM\\CurrentControlSet\\Control\\CI\\Config", "VulnerableDriverBlocklistEnable", 0);
            return true;
        } catch (Exception e) {
            System.out.println("    [FAILED] " + e.getMessage());
            return false;
        }
    }

    /**
     * Attempts to disable User Account Control (UAC).
     */
    public boolean fixUAC() {
        System.out.println(" -> Attempting to disable UAC...");
        try {
            Advapi32Util.registrySetIntValue(WinReg.HKEY_LOCAL_MACHINE,
                    "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\System", "EnableLUA", 0);
            return true;
        } catch (Exception e) {
            System.out.println("    [FAILED] " + e.getMessage());
            return false;
        }
    }
}
