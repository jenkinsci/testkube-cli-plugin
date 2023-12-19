package io.testkube.adapters;

import hudson.Extension;
import hudson.model.TaskListener;

import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;

import java.util.Collections;
import java.util.Set;

public class TestkubeStep extends Step {

    @DataBoundConstructor
    public TestkubeStep() {
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new TestkubeStepExecution(context);
    }

    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "setupTestkube";
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Testkube Setup Step";
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
