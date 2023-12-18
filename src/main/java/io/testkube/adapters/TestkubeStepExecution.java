package io.testkube.adapters;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import hudson.EnvVars;

import hudson.model.TaskListener;
import io.testkube.setup.TestkubeSetup;

public class TestkubeStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    protected TestkubeStepExecution(StepContext context) {
        super(context);
    }

    @Override
    protected Void run() throws Exception {
        var envVars = getContext().get(EnvVars.class);
        var logger = getContext().get(TaskListener.class).getLogger();

        TestkubeSetup testkubeSetup = new TestkubeSetup(logger, envVars);
        try {
            testkubeSetup.setup();
        } catch (Exception e) {
            logger.println("Error during Testkube setup: " + e.getMessage());
            getContext().onFailure(null);
            return null;
        }

        getContext().onSuccess(null);
        return null;
    }
}
