package io.testkube.plugins.api.builders;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import io.testkube.plugins.api.manager.TestkubeConfig;
import io.testkube.plugins.api.manager.TestkubeManager;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

public class TestkubeTestRunnerBuilder extends Builder {
    private String testName;

    @DataBoundConstructor
    public TestkubeTestRunnerBuilder(String apiUrl, String orgId, String envId, String tkNamespace, String apiToken,
            String testName) {
        this.testName = testName;
        TestkubeConfig.init(orgId, apiUrl, envId, apiToken, tkNamespace);
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("Start Testkube test");

        var executionId = TestkubeManager.runTest(testName);

        var result = TestkubeManager.waitForExecution(testName, executionId);

        if (result.getStatus().equals("passed")) {
            return true;
        }

        return false;
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
            return "Testkube Test Runner";
        }
    }
}
