package io.jenkins.plugins.testkube.cli.adapters;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import io.jenkins.plugins.testkube.cli.setup.TestkubeCLI;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.kohsuke.stapler.DataBoundConstructor;

public class TestkubeBuilder extends Builder {

    @DataBoundConstructor
    public TestkubeBuilder() {}

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        EnvVars envVars;
        try {
            envVars = build.getEnvironment(listener);
        } catch (IOException | InterruptedException e) {
            listener.getLogger().println("Error getting environment variables: " + e.getMessage());
            return false;
        }

        TestkubeCLI testkubeCLI = new TestkubeCLI(listener.getLogger(), envVars);
        var success = testkubeCLI.setup();

        return success;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Testkube Setup";
        }
    }
}
