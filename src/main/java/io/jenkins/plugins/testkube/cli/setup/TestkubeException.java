package io.jenkins.plugins.testkube.cli.setup;

import java.util.Collections;
import java.util.List;

public class TestkubeException extends Exception {
    private final String userMessage;
    private final List<String> possibleSolutions;

    public TestkubeException(String title, String details) {
        this(title, details, Collections.emptyList());
    }

    public TestkubeException(String title, String details, List<String> solutions) {
        super(formatMessage(title, details, solutions));
        this.userMessage = details;
        this.possibleSolutions = solutions != null ? solutions : Collections.emptyList();
    }

    private static String formatMessage(String title, String details, List<String> solutions) {
        StringBuilder message = new StringBuilder();
        message.append(title).append("\n\n");
        message.append("Details: ").append(details).append("\n\n");

        if (solutions != null && !solutions.isEmpty()) {
            message.append("Possible solutions:\n");
            for (int i = 0; i < solutions.size(); i++) {
                message.append(String.format("%d. %s\n", i + 1, solutions.get(i)));
            }
        }

        return message.toString();
    }

    public String getUserMessage() {
        return userMessage;
    }

    public List<String> getPossibleSolutions() {
        return Collections.unmodifiableList(possibleSolutions);
    }
}