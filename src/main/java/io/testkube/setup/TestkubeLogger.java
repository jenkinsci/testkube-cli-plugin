package io.testkube.setup;

import java.io.PrintStream;

public class TestkubeLogger {
    private static TestkubeLogger instance = null;

    private PrintStream printStream;

    private TestkubeLogger() {
        // private constructor to prevent instantiation
    }

    private static TestkubeLogger getInstance() {
        if (instance == null) {
            instance = new TestkubeLogger();
        }
        return instance;
    }

    public static void init(PrintStream printStream) {
        getInstance().printStream = printStream;
    }

    public static void println(String msg) {
        if (getInstance().printStream == null) {
            return;
        }
        getInstance().printStream.println("[Testkube] " + msg);
    }

    public static PrintStream getPrintStream() {
        return getInstance().printStream;
    }

}