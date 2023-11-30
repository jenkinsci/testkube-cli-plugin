package io.testkube.plugins;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.annotation.Nonnull;
import java.io.IOException;

public class TestkubeTestRunnerBuilder extends Builder {

    private final String apiUrl;
    private final String orgId;
    private final String envId;
    private final String tkNamespace;
    private final String apiToken;
    private final String testName;

    @DataBoundConstructor
    public TestkubeTestRunnerBuilder(String apiUrl, String orgId, String envId, String tkNamespace, String apiToken,
            String testName) {
        this.apiUrl = apiUrl;
        this.orgId = orgId;
        this.envId = envId;
        this.tkNamespace = tkNamespace;
        this.apiToken = apiToken;
        this.testName = testName;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("Start Testkube test");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String fullUrl = apiUrl + "/organizations/" + orgId + "/environments/" + envId + "/agent/tests/" + testName
                    + "/executions";

            listener.getLogger().println("Testkube Test URL: " + fullUrl);

            HttpPost request = new HttpPost(fullUrl);
            request.setHeader("Authorization", "Bearer " + apiToken);
            request.setHeader("Content-Type", "application/json");

            String jsonBody = "{\"namespace\": \"" + tkNamespace + "\"}";
            StringEntity entity = new StringEntity(jsonBody);
            request.setEntity(entity);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                listener.getLogger().println("Status: " + response.getStatusLine());
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

        @Nonnull
        @Override
        public String getDisplayName() {
            return "Testkube Runner";
        }
    }
}
