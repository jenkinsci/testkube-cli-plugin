package io.testkube.plugins.cli;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

public class WithTestkubeStep extends Step {

    private String orgId;
    private String envId;
    private String apiToken;
    private String agentToken;
    private String rootDomain;

    @DataBoundConstructor
    public WithTestkubeStep() {
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

    public String getRootDomain() {
        return rootDomain != null ? rootDomain : "testkube.io";
    }

    @DataBoundSetter
    public void setRootDomain(String rootDomain) {
        this.rootDomain = rootDomain;
    }

    public String getAgentToken() {
        return agentToken;
    }

    @DataBoundSetter
    public void setAgentToken(String agentToken) {
        this.agentToken = agentToken;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        var envVars = context.get(EnvVars.class);
        var TESTKUBE_ORG_ID = envVars.get("TESTKUBE_ORG_ID");
        if (orgId == null && TESTKUBE_ORG_ID != null) {
            orgId = TESTKUBE_ORG_ID;
        }
        var TESTKUBE_ENV_ID = envVars.get("TESTKUBE_ENV_ID");
        if (envId == null && TESTKUBE_ENV_ID != null) {
            envId = TESTKUBE_ENV_ID;
        }
        var TESTKUBE_API_TOKEN = envVars.get("TESTKUBE_API_TOKEN");
        if (apiToken == null && TESTKUBE_API_TOKEN != null) {
            apiToken = TESTKUBE_API_TOKEN;
        }
        var TESTKUBE_AGENT_TOKEN = envVars.get("TESTKUBE_AGENT_TOKEN");
        if (agentToken == null && TESTKUBE_AGENT_TOKEN != null) {
            agentToken = TESTKUBE_AGENT_TOKEN;
        }
        var TESTKUBE_ROOT_DOMAIN = envVars.get("TESTKUBE_ROOT_DOMAIN");
        if (rootDomain == null && TESTKUBE_ROOT_DOMAIN != null) {
            rootDomain = TESTKUBE_ROOT_DOMAIN;
        }
        return new WithTestkubeStepExecution(this, context);
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
