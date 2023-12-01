package io.testkube.plugins.cli;

import org.jenkinsci.plugins.workflow.steps.BodyInvoker;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;

import hudson.AbortException;
import hudson.Launcher;
import hudson.Launcher.ProcStarter;
import hudson.Proc;
import hudson.model.TaskListener;

public class WithTestkubeStepExecution extends SynchronousNonBlockingStepExecution<Void> {
    private static final long serialVersionUID = 1L;

    private transient final WithTestkubeStep step;

    protected WithTestkubeStepExecution(WithTestkubeStep step, StepContext context) {
        super(context);
        this.step = step;
    }

    @Override
    protected Void run() throws Exception {
        TaskListener listener = getContext().get(TaskListener.class);
        listener.getLogger().println("Executing withTestkube with orgId: " + step.getOrgId() + ", envId: "
                + step.getEnvId() + ", apiToken: " + step.getApiToken());
        Launcher launcher = getContext().get(Launcher.class);
        ProcStarter procStarter = launcher.launch();
        procStarter.stdout(listener).cmds("curl", "-sSLf", "https://get.testkube.io");
        Proc proc = procStarter.start();
        int exitCode = proc.join();
        if (exitCode != 0) {
            throw new AbortException("Command execution failed with exit code: " + exitCode);
        }

        // Run the testkube set context command
        ProcStarter testkubeInitProcStarter = launcher.launch();
        testkubeInitProcStarter.stdout(listener).cmds("testkube", "cloud", "init", "--agent-token",
                step.getAgentToken(),
                "--org-id", step.getOrgId(), "--env-id", step.getEnvId(), "--cloud-root-domain", "testkube.dev",
                "--no-confirm");
        Proc testkubeInitProc = testkubeInitProcStarter.start();
        int testkubeInitExitCode = testkubeInitProc.join();
        if (testkubeInitExitCode != 0) {
            throw new AbortException(
                    "'testkube cloud init' execution failed with exit code: " + testkubeInitExitCode);
        }

        // Run the testkube set context command
        ProcStarter testkubeSetContextProcStarter = launcher.launch();
        testkubeSetContextProcStarter.stdout(listener).cmds("testkube", "set", "context", "-c", "cloud", "-e",
                step.getEnvId(),
                "-o", step.getOrgId(), "-k", step.getApiToken(), "--cloud-root-domain", "testkube.dev");
        Proc testkubeSetContextProc = testkubeSetContextProcStarter.start();
        int testkubeSetContextExitCode = testkubeSetContextProc.join();
        if (testkubeSetContextExitCode != 0) {
            throw new AbortException(
                    "'testkube set context' command execution failed with exit code: " + testkubeSetContextExitCode);
        }

        BodyInvoker bodyInvoker = getContext().newBodyInvoker();
        bodyInvoker.withContexts(listener, launcher).start();

        getContext().onSuccess(null);
        return null;
    }
}
