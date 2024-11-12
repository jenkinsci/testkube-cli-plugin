package io.jenkins.plugins.testkube.cli.setup;

import java.io.PrintStream;
import java.util.List;

public class TestkubeLogger {
    private static PrintStream printStream;
    private static boolean debug;

    public static void setPrintStream(PrintStream stream) {
        printStream = stream;
    }

    public static void setDebug(boolean debugMode) {
        debug = debugMode;
    }

    public static void debug(String msg) {
        if (printStream != null && debug) {
            printStream.println("[Testkube][DEBUG] " + msg);
        }
    }

    public static void println(String msg) {
        if (printStream != null) {
            printStream.println("[Testkube] " + msg);
        }
    }

    public static void error(String msg, Throwable error) {
        if (printStream == null) {
            return;
        }

        // Print main error message
        printStream.println("[Testkube] ERROR: " + msg);

        // Handle TestkubeException specially
        if (error instanceof TestkubeException) {
            TestkubeException te = (TestkubeException) error;

            // Print details if available
            if (te.getDetails() != null && !te.getDetails().isEmpty()) {
                printStream.println("\nDetails: " + te.getDetails());
            }

            // Print solutions if available
            List<String> solutions = te.getPossibleSolutions();
            if (!solutions.isEmpty()) {
                printStream.println("\nPossible solutions:");
                for (int i = 0; i < solutions.size(); i++) {
                    printStream.println((i + 1) + ". " + solutions.get(i));
                }
            }
        }

        // Handle stack trace based on debug mode
        if (debug) {
            printStream.println("\nFull error details (debug mode):");
            error.printStackTrace(printStream);
        } else {
            printStream.println("\nTip: Run with TK_DEBUG=true to see full stack trace");
        }
    }

    public static PrintStream getPrintStream() {
        return printStream;
    }
}
