package com.dmacheese.pccheck;

/**
 * Application entry point.
 * Initializes service components and starts the console runner.
 */
public class Main {

    public static void main(String[] args) {
        SystemCheckService checkService = new SystemCheckService();
        FixService fixService = new FixService();
        ConsoleRunner runner = new ConsoleRunner(checkService, fixService);

        runner.run(args);
    }
}
