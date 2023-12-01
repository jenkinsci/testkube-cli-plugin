package io.testkube.plugins.api.manager;

public enum TestkubeTestType {
    TEST("tests"),
    TEST_SUITE("test-suites");

    private final String value;

    TestkubeTestType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}