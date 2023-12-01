package io.testkube.plugins.api.manager;

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
        Utils.checkNotNull("logger", logger);

        getInstance().logger = logger;
    }

    public static void println(String msg) {
        checkInitialized();
        getInstance().logger.println(msg);
    }

    private static void checkInitialized() {
        if (getInstance().logger == null) {
            throw new IllegalStateException(
                    "TestkubeLogger has not been initialized. Call TestkubeLogger.init() first.");
        }
    }
}