package io.testkube.setup;

import java.io.PrintStream;

public class TestkubeLogger {
    private static TestkubeLogger instance = null;

    private PrintStream logger;

    private TestkubeLogger() {
        // private constructor to prevent instantiation
    }

    private static TestkubeLogger getInstance() {
        if (instance == null) {
            instance = new TestkubeLogger();
        }
        return instance;
    }

    public static void init(PrintStream logger) {
        getInstance().logger = logger;
    }

    public static void println(String msg) {
        if (getInstance().logger == null) {
            return;
        }
        getInstance().logger.println(msg);
    }

}