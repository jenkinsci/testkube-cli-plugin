package io.testkube.plugins.api.steps;

import hudson.Extension;
import hudson.model.TaskListener;

import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

public class RunTestStep extends Step {

    private final String testName;

    @DataBoundConstructor
    public RunTestStep(String testName) {
        this.testName = testName;
    }

    public String getTestName() {
        return testName;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new RunTestExecution(this, context);

    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "testkubeRunTest";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Testkube Run Test Step";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.singleton(TaskListener.class);
        }
    }
}
