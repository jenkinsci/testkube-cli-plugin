package io.testkube.plugins.api.steps;

import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import hudson.model.TaskListener;
import io.testkube.plugins.api.manager.TestkubeLogger;
import io.testkube.plugins.api.manager.TestkubeManager;

public class RunTestExecution extends SynchronousNonBlockingStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    private transient final RunTestStep step;

    protected RunTestExecution(RunTestStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    protected Void run() throws Exception {
        TaskListener listener = getContext().get(TaskListener.class);
        var logger = listener.getLogger();
        TestkubeLogger.init(logger);

        var runResult = TestkubeManager.runTest(step.getTestName());
        var result = TestkubeManager.waitForExecution(runResult.getTestName(), runResult.getExecutionId());

        if (result == null || !result.getStatus().equals("passed")) {
            getContext().onFailure(null);
            return null;
        }

        getContext().onSuccess(null);
        return null;
    }
}
