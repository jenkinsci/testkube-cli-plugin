package io.testkube.plugins.api.manager;

public class TestkubeRunResult {

    private final String testName;
    private final String executionId;

    public TestkubeRunResult(String testName, String executionId) {
        this.testName = testName;
        this.executionId = executionId;
    }

    public String getTestName() {
        return testName;
    }

    public String getExecutionId() {
        return executionId;
    }

    @Override
    public String toString() {
        String newLine = System.lineSeparator();
        return "Test Name: " + testName + newLine + "Execution ID: " + executionId + newLine;
    }
}
