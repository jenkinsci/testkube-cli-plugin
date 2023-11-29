package io.testkube.plugins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.IOException;

public class TestkubeTestRunnerBuilder extends Builder {

    private final String apiUrl;
    private final String apiToken;
    private final String testName;

    @DataBoundConstructor
    public TestkubeTestRunnerBuilder(String apiUrl, String apiToken, String testName) {
        this.apiUrl = apiUrl;
        this.apiToken = apiToken;
        this.testName = testName;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("Making REST API Call to: " + apiUrl);
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(apiUrl);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                listener.getLogger().println("Response: " + response.getStatusLine());
                // Handle the response as needed
            }
        } catch (IOException e) {
            listener.getLogger().println("Error during REST API Call: " + e.getMessage());
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

        @Override
        public String getDisplayName() {
            return "Testkube Runner";
        }
    }
}
