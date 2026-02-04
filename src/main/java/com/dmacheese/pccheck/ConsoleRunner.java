package com.dmacheese.pccheck;

import java.util.Scanner;

/**
 * Main console runner that orchestrates system checks and user interaction.
 * Uses dependency injection for service components.
 */
public class ConsoleRunner {

    private final SystemCheckService checkService;
    private final FixService fixService;

    public ConsoleRunner(SystemCheckService checkService, FixService fixService) {
        this.checkService = checkService;
        this.fixService = fixService;
    }

    /**
     * Runs the PC compatibility checker application.
     */
    public void run(String... args) {
        com.sun.jna.platform.win32.Kernel32.INSTANCE.SetConsoleTitle("DMACHEESE.COM PC CHECKER");

        printHeader();

        if (!Utils.isAdministrator()) {
            Utils.setConsoleColor(Utils.Color.Red);
            System.out.println("WARNING: Application is not running as Administrator!");
            System.out.println("Automated fixes will not work and some checks may result in false negatives.");
            System.out.println("Please restart as Administrator.");
            Utils.setConsoleColor(Utils.Color.Default);
            System.out.println();
        }

        System.out.println("Checking system requirements for DMA...\n");

        boolean criticalPass = true;
        boolean optionalPass = true;

        // Critical Checks
        Utils.setConsoleColor(Utils.Color.Yellow);
        System.out.println("[ CRITICAL CHECKS ]");
        Utils.setConsoleColor(Utils.Color.Default);

        if (!checkService.checkFTDIBUS())
            criticalPass = false;

        System.out.println();

        // Recommended Checks
        Utils.setConsoleColor(Utils.Color.Yellow);
        System.out.println("[ RECOMMENDED CHECKS (Nice to have) ]");
        Utils.setConsoleColor(Utils.Color.Default);

        boolean failWinSec = !checkService.checkWindowsSecurity();
        if (failWinSec)
            optionalPass = false;

        boolean failHVCI = !checkService.checkHVCI();
        if (failHVCI)
            optionalPass = false;

        boolean failVuln = !checkService.checkVulnDriverBlocklist();
        if (failVuln)
            optionalPass = false;

        boolean failUAC = !checkService.checkUAC();
        if (failUAC)
            optionalPass = false;

        if (!checkService.checkKernelDMAProtection())
            optionalPass = false;
        if (!checkService.checkVCRuntimes())
            optionalPass = false;
        if (!checkService.checkDirectX())
            optionalPass = false;

        // Offer fixes if any recommended checks failed
        if (!optionalPass && (failWinSec || failHVCI || failVuln || failUAC)) {
            System.out.println("\n[!] Some recommended settings are incorrect.");
            System.out.print(
                    "Do you want to attempt to fix these issues? (Windows Security, HVCI, VulnDriver, UAC) [Y/n]: ");

            Scanner scanner = new Scanner(System.in);
            String response = scanner.next();

            if (response.equalsIgnoreCase("y")) {
                System.out.println("\n--- Applying Fixes ---");
                if (!Utils.isAdministrator()) {
                    System.out.println("ERROR: Cannot apply fixes without Administrator privileges.");
                } else {
                    if (failWinSec)
                        fixService.fixWindowsSecurity();
                    if (failHVCI)
                        fixService.fixHVCI();
                    if (failVuln)
                        fixService.fixVulnDriverBlocklist();
                    if (failUAC)
                        fixService.fixUAC();

                    System.out.println("\n[INFO] Fixes applied. Please REBOOT your PC to apply these changes fully.");
                }
            }
        }

        // Print final result
        System.out.println("\n======================================================================");

        if (criticalPass && optionalPass) {
            Utils.setConsoleColor(Utils.Color.Green);
            System.out.println("            [PASS] SYSTEM IS PERFECTLY READY FOR DMACHEESE  ");
        } else if (criticalPass) {
            Utils.setConsoleColor(Utils.Color.Yellow);
            System.out.println("            [WARN] SYSTEM IS READY (WITH WARNINGS)          ");
            System.out.println("            Critical checks passed. Speedtest allowed.      ");
            System.out.println("            (Performance might be better if you fix optional issues)");
        } else {
            Utils.setConsoleColor(Utils.Color.Red);
            System.out.println("            [FAIL] CRITICAL CHECKS FAILED                   ");
            System.out.println("            Cannot proceed to speedtest without drivers.    ");
        }
        Utils.setConsoleColor(Utils.Color.Default);
        System.out.println("======================================================================");

        Utils.pressEnterToExit();
    }

    private void printHeader() {
        Utils.setConsoleColor(Utils.Color.Yellow);
        System.out.println(
                "  ____  __  __    _    ____ _   _ _____ _____ ____  _____     ____ ___  __  __\n" +
                        " |  _ \\|  \\/  |  / \\  / ___| | | | ____| ____/ ___|| ____|   / ___/ _ \\|  \\/  |\n" +
                        " | | | | |\\/| | / _ \\| |   | |_| |  _| |  _| \\___ \\|  _|    | |  | | | | |\\/| |\n" +
                        " | |_| | |  | |/ ___ \\ |___|  _  | |___| |___ ___) | |___   | |__| |_| | |  | |\n" +
                        " |____/|_|  |_/_/   \\_\\____|_| |_|_____|_____|____/|_____|   \\____\\___/|_|  |_|\n" +
                        "\n" +
                        "                      PC COMPATIBILITY CHECKER (JAVA EDITION)\n" +
                        "======================================================================");
        Utils.setConsoleColor(Utils.Color.Default);
        System.out.println();
    }
}
