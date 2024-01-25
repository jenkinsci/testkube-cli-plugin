package io.jenkins.plugins.testkube.cli.adapters;

import hudson.EnvVars;
import hudson.model.TaskListener;
import io.jenkins.plugins.testkube.cli.setup.TestkubeCLI;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

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
            getContext().onFailure(new Exception("Testkube CLI setup failed"));
            return null;
        }

        return null;
    }
}
