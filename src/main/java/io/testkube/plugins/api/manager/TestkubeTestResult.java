package io.testkube.plugins.api.manager;

public class TestkubeTestResult {

    private String executionId;
    private String status;
    private String output;
    private String errorMessage;

    public TestkubeTestResult(String executionId, String status, String output, String errorMessage) {
        this.executionId = executionId;
        this.status = status;
        this.output = output;
        this.errorMessage = errorMessage;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getStatus() {
        return status;
    }

    public String getOutput() {
        return output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        String newLine = System.lineSeparator();
        return "Testkube Test Execution Result " + newLine +
                "Result Execution ID: " + executionId + newLine +
                "Status: " + status + newLine +
                "Output: " + newLine + output + newLine +
                "Error: " + newLine + errorMessage + newLine;
    }
}
