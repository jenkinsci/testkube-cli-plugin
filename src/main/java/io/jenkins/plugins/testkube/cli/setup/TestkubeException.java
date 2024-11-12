package io.jenkins.plugins.testkube.cli.setup;

import java.util.Collections;
import java.util.List;

public class TestkubeException extends Exception {
    private final String title;
    private final String details;
    private final List<String> possibleSolutions;

    public TestkubeException(String title, String details) {
        this(title, details, Collections.emptyList());
    }

    public TestkubeException(String title, String details, List<String> solutions) {
        super(formatMessage(title, details));
        this.title = title;
        this.details = details;
        this.possibleSolutions = solutions != null ? solutions : Collections.emptyList();
    }

    private static String formatMessage(String title, String details) {
        return title + ": " + details;
    }

    public String getTitle() {
        return title;
    }

    public String getDetails() {
        return details;
    }

    public List<String> getPossibleSolutions() {
        return Collections.unmodifiableList(possibleSolutions);
    }
}
