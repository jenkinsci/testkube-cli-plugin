package io.testkube.plugins;

import hudson.model.TaskListener;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

public class TestkubeStepExecution extends SynchronousStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    private transient final TestkubeStep step;

    protected TestkubeStepExecution(TestkubeStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    protected Void run() throws Exception {
        TaskListener listener = getContext().get(TaskListener.class);

        listener.getLogger().println("Executing withTestkube with apiToken: " + step.getApiToken());

        getContext().onSuccess(null);

        return null;
    }
}
