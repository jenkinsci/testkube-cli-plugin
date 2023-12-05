package io.testkube.plugins.api.steps;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.TaskListener;
import io.testkube.plugins.api.manager.TestkubeConfig;
import io.testkube.plugins.api.manager.TestkubeLogger;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.Set;

public class InitStep extends Step {

    private String orgId;
    private String envId;
    private String apiToken;
    private String apiUrl;
    private String namespace;

    @DataBoundConstructor
    public InitStep() {
    }

    public String getOrgId() {
        return orgId;
    }

    @DataBoundSetter
    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getEnvId() {
        return envId;
    }

    @DataBoundSetter
    public void setEnvId(String envId) {
        this.envId = envId;
    }

    public String getApiToken() {
        return apiToken;
    }

    @DataBoundSetter
    public void setApiToken(String apiToken) {
        this.apiToken = apiToken;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    @DataBoundSetter
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getNamespace() {
        return namespace;
    }

    @DataBoundSetter
    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        var logger = context.get(TaskListener.class).getLogger();
        TestkubeLogger.init(logger);
        TestkubeConfig.setApiUrl(apiUrl);
        TestkubeConfig.setApiToken(apiToken);
        TestkubeConfig.setOrgId(orgId);
        TestkubeConfig.setEnvId(envId);
        TestkubeConfig.setNamespace(namespace);
        TestkubeConfig.init(context.get(EnvVars.class));
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
