package io.testkube.adapters;

import hudson.EnvVars;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import io.testkube.setup.TestkubeSetup;
import hudson.tasks.BuildStepDescriptor;

import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

import javax.annotation.Nonnull;

public class TestkubeBuilder extends Builder {

    @DataBoundConstructor
    public TestkubeBuilder() {
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        EnvVars envVars;
        try {
            envVars = build.getEnvironment(listener);
        } catch (IOException | InterruptedException e) {
            listener.getLogger().println("Error getting environment variables: " + e.getMessage());
            return false;
        }

        TestkubeSetup testkubeSetup = new TestkubeSetup(listener.getLogger(), envVars);
        try {
            testkubeSetup.setup(envVars);
        } catch (Exception e) {
            listener.getLogger().println("Error during Testkube setup: " + e.getMessage());
            return false;
        }

        return true;
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
