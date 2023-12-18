package io.testkube.setup;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.json.JSONArray;
import org.json.JSONObject;

public class TestkubeDetectors {

    public static void detectKubectl(boolean isCloudMode) throws Exception {
        if (!isCloudMode) {
            ProcessBuilder processBuilder = new ProcessBuilder("kubectl", "version", "--client");
            try {
                Process process = processBuilder.start();
                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    TestkubeLogger.println("kubectl: detected.");
                } else {
                    throw new Exception(
                            "You do not have kubectl installed. Most likely you need to configure your workflow to initialize connection with Kubernetes cluster.");
                }
            } catch (IOException | InterruptedException e) {
                throw new Exception("kubectl: not available.");
            }
        } else {
            TestkubeLogger.println("Testkube: kubectl ignored for Cloud integration");
        }
    }

    public static Boolean detectTestkubeCLI(String channel, String forcedVersion)
            throws Exception {
        // Check if kubectl-testkube is installed
        ProcessBuilder processBuilder = new ProcessBuilder("which", "kubectl-testkube");
        boolean isInstalled = false;
        try {
            Process process = processBuilder.start();
            isInstalled = process.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            // Ignore
        }

        if (isInstalled) {
            TestkubeLogger.println("Looks like you already have the Testkube CLI installed. Skipping...");
            return true;
        }

        return false;
    }

    public static String detectTestkubeVersion(String channel) throws Exception {

        TestkubeLogger.println("Detecting the latest version for minimum of \"" + channel + "\" channel...");
        String version = null;
        HttpClient client = HttpClient.newHttpClient();
        String releasesUrl = "https://api.github.com/repos/kubeshop/testkube/releases";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(channel.equals("stable") ? releasesUrl + "/latest" : releasesUrl)).build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (channel.equals("stable")) {
            JSONObject release = new JSONObject(response.body());
            version = release.optString("tag_name", null);
        } else {
            JSONArray releases = new JSONArray(response.body());
            for (int i = 0; i < releases.length(); i++) {
                JSONObject release = releases.getJSONObject(i);
                String tag = release.getString("tag_name");
                String releaseChannel = tag.matches("-([^0-9]+)") ? tag.replaceAll("-([^0-9]+)", "$1") : "stable";
                if (releaseChannel.equals(channel) || releaseChannel.equals("stable")) {
                    version = tag;
                    break;
                }
            }
        }

        if (version == null) {
            throw new IOException("Not found any version matching criteria.");
        }

        version = version.replaceFirst("^v", "");

        TestkubeLogger.println("   Latest version: " + version);

        return version;
    }

    public static String detectSystem() throws Exception {
        String osName = System.getProperty("os.name");
        switch (osName) {
            case "Linux":
                return "Linux";
            case "Mac OS X":
                return "Darwin";
            case "Windows 10":
            case "Windows 7":
                return "Windows";
            default:
                throw new Exception("We do not support this OS yet.");
        }
    }

    public static String detectArchitecture() throws Exception {
        String osArch = System.getProperty("os.arch");
        switch (osArch) {
            case "x86_64":
            case "amd64":
                return "x86_64";
            case "aarch64":
                return "arm64";
            case "i386":
                return "i386";
            default:
                throw new Exception("We do not support this architecture yet.");
        }
    }

}
