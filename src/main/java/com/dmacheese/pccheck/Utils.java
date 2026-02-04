package com.dmacheese.pccheck;

import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.win32.W32APIOptions;

import java.io.IOException;

/**
 * Utility class for console output formatting and Windows API interactions.
 */
public class Utils {

    public enum Color {
        Red, Green, Yellow, White, Default
    }

    /**
     * Custom JNA interface for Shell32 functions.
     */
    public interface MyShell32 extends Library {
        MyShell32 INSTANCE = Native.load("shell32", MyShell32.class, W32APIOptions.DEFAULT_OPTIONS);

        boolean IsUserAnAdmin();
    }

    /**
     * Checks if the application is running with administrator privileges.
     */
    public static boolean isAdministrator() {
        try {
            return MyShell32.INSTANCE.IsUserAnAdmin();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * Custom JNA interface for Kernel32 console functions.
     */
    public interface MyKernel32 extends Library {
        MyKernel32 INSTANCE = Native.load("kernel32", MyKernel32.class, W32APIOptions.DEFAULT_OPTIONS);

        WinNT.HANDLE GetStdHandle(int nStdHandle);

        boolean SetConsoleTextAttribute(WinNT.HANDLE hConsoleOutput, short wAttributes);
    }

    private static final int STD_OUTPUT_HANDLE = -11;
    private static final short FOREGROUND_BLUE = 0x0001;
    private static final short FOREGROUND_GREEN = 0x0002;
    private static final short FOREGROUND_RED = 0x0004;
    private static final short FOREGROUND_INTENSITY = 0x0008;

    /**
     * Sets the console text color using Windows API.
     */
    public static void setConsoleColor(Color c) {
        HANDLE hConsole = MyKernel32.INSTANCE.GetStdHandle(STD_OUTPUT_HANDLE);
        short attributes;

        switch (c) {
            case Red:
                attributes = (short) (FOREGROUND_RED | FOREGROUND_INTENSITY);
                break;
            case Green:
                attributes = (short) (FOREGROUND_GREEN | FOREGROUND_INTENSITY);
                break;
            case Yellow:
                attributes = (short) (FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_INTENSITY);
                break;
            case White:
                attributes = (short) (FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE | FOREGROUND_INTENSITY);
                break;
            case Default:
            default:
                attributes = (short) (FOREGROUND_RED | FOREGROUND_GREEN | FOREGROUND_BLUE);
                break;
        }

        MyKernel32.INSTANCE.SetConsoleTextAttribute(hConsole, attributes);
    }

    /**
     * Prints a formatted check result to the console with color coding.
     */
    public static void printResult(String checkName, boolean passed, String message) {
        System.out.print("[");
        if (passed) {
            setConsoleColor(Color.Green);
            System.out.print(" OK ");
        } else {
            setConsoleColor(Color.Red);
            System.out.print("FAIL");
        }

        setConsoleColor(Color.Default);
        System.out.print("] " + checkName);

        if (message != null && !message.isEmpty()) {
            System.out.print(" (");
            setConsoleColor(passed ? Color.Green : Color.Red);
            System.out.print(message);
            setConsoleColor(Color.Default);
            System.out.print(")");
        }
        System.out.println();
    }

    /**
     * Pauses execution until user presses Enter.
     */
    public static void pressEnterToExit() {
        System.out.println("\nPress Enter to exit...");
        try {
            System.in.read();
        } catch (IOException e) {
            // User closed input stream
        }
    }
}
