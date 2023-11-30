package io.testkube.plugins;

import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

public class TestkubeStep extends Step {

    private final String orgId;
    private final String envId;
    private final String apiToken;
    private final String agentToken;

    @DataBoundConstructor
    public TestkubeStep(String orgId, String envId, String apiToken, String agentToken) {
        this.orgId = orgId;
        this.envId = envId;
        this.apiToken = apiToken;
        this.agentToken = agentToken;
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

    public String getAgentToken() {
        return agentToken;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new TestkubeStepExecution(this, context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "withTestkube";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Testkube Step";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true;
        }
    }
}
