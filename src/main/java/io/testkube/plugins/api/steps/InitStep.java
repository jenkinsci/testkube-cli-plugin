package io.testkube.plugins.api.steps;

import hudson.Extension;
import hudson.model.TaskListener;
import io.testkube.plugins.api.manager.TestkubeConfig;

import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

public class InitStep extends Step {

    private final String orgId;
    private final String envId;
    private final String apiToken;
    private final String apiUrl;

    @DataBoundConstructor
    public InitStep(String orgId, String envId, String apiToken, String apiUrl) {
        this.orgId = orgId;
        this.envId = envId;
        this.apiToken = apiToken;
        this.apiUrl = apiUrl;
    }

    public String getOrgId() {
        return orgId;
    }

    public String getEnvId() {
        return envId;
    }

    public String getApiToken() {
        return apiToken;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        TestkubeConfig.init(orgId, apiUrl, envId, apiToken, "testkube");
        return new NoOpExecution(context);
    }

    private static class NoOpExecution extends SynchronousNonBlockingStepExecution<Void> {
        protected NoOpExecution(StepContext context) {
            super(context);
        }

        @Override
        protected Void run() throws Exception {
            // Do nothing
            return null;
        }
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "testkubeInit";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Testkube Init Step";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }
    }
}
