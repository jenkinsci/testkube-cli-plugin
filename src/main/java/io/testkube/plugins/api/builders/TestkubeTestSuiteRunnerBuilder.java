package io.testkube.plugins.api.builders;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import io.testkube.plugins.api.manager.TestkubeConfig;
import io.testkube.plugins.api.manager.TestkubeLogger;
import io.testkube.plugins.api.manager.TestkubeManager;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

import javax.annotation.Nonnull;

public class TestkubeTestSuiteRunnerBuilder extends Builder {
    private final String testName;

    @DataBoundConstructor
    public TestkubeTestSuiteRunnerBuilder(String testName) {
        this.testName = testName;

    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        var logger = listener.getLogger();

        EnvVars envVars;
        try {
            envVars = build.getEnvironment(listener);
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }

        TestkubeLogger.init(logger);
        TestkubeConfig.init(envVars);

        var runResults = TestkubeManager.runTestSuite(testName);

        for (var runResult : runResults) {
            var result = TestkubeManager.waitForExecution(runResult.getTestName(), runResult.getExecutionId());
            if (result == null || !result.getStatus().equals("passed")) {
                return false;
            }
        }

        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Testkube Test Suite Runner";
        }
    }
}
