package io.testkube.adapters;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import hudson.EnvVars;

import hudson.model.TaskListener;
import io.testkube.setup.TestkubeCLI;

public class TestkubeStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    protected TestkubeStepExecution(StepContext context) {
        super(context);
    }

    @Override
    protected Void run() throws Exception {
        var envVars = getContext().get(EnvVars.class);
        var logger = getContext().get(TaskListener.class).getLogger();

        TestkubeCLI testkubeCLI = new TestkubeCLI(logger, envVars);
        var success = testkubeCLI.setup();

        if (success) {
            getContext().onSuccess(null);

        } else {
            getContext().onFailure(null);

        }

        return null;
    }
}
