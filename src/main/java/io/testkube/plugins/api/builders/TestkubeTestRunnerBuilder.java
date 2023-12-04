package io.testkube.plugins.api.builders;

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

import javax.annotation.Nonnull;

public class TestkubeTestRunnerBuilder extends Builder {
    private final String testName;
    private final String apiUrl;
    private final String orgId;
    private final String envId;
    private final String tkNamespace;
    private final String apiToken;

    @DataBoundConstructor
    public TestkubeTestRunnerBuilder(String apiUrl, String orgId, String envId, String tkNamespace, String apiToken,
            String testName) {
        this.testName = testName;
        this.apiUrl = apiUrl;
        this.orgId = orgId;
        this.envId = envId;
        this.tkNamespace = tkNamespace;
        this.apiToken = apiToken;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        var logger = listener.getLogger();
        TestkubeConfig.init(orgId, apiUrl, envId, apiToken, tkNamespace);
        TestkubeLogger.init(logger);

        var runResult = TestkubeManager.runTest(testName);
        var result = TestkubeManager.waitForExecution(runResult.getTestName(), runResult.getExecutionId());

        if (result == null) {
            return false;
        }

        return result.getStatus().equals("passed");
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
