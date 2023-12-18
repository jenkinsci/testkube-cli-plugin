package io.testkube.setup;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

import hudson.EnvVars;

public class TestkubeSetup {
    private String version;
    private String channel;
    private String namespace;
    private String url;
    private String organization;
    private String environment;
    private String token;

    public TestkubeSetup(PrintStream logger, EnvVars envVars) {
        TestkubeLogger.init(logger);
        this.version = envVars.get("TESTKUBE_VERSION");
        this.channel = envVars.get("TESTKUBE_CHANNEL");
        this.namespace = envVars.get("TESTKUBE_NAMESPACE");
        this.url = envVars.get("TESTKUBE_URL");
        this.organization = envVars.get("TESTKUBE_ORGANIZATION");
        this.environment = envVars.get("TESTKUBE_ENVIRONMENT");
        this.token = envVars.get("TESTKUBE_TOKEN");
    }

    public TestkubeSetup(PrintStream logger, String version, String channel, String namespace, String url,
            String organization,
            String environment, String token) {
        TestkubeLogger.init(logger);
        this.channel = channel;
        this.namespace = namespace;
        this.url = url;
        this.version = version;
        this.organization = organization;
        this.environment = environment;
        this.token = token;
    }

    private void setDefaults() {
        if (namespace == null) {
            namespace = "testkube";
        }
        if (channel == null) {
            channel = "stable";
        }
        if (url == null) {
            url = "testkube.io";
        }
    }

    public void setup() throws Exception {
        setDefaults();

        Boolean isCloudMode = (organization != null || environment != null || token != null) ? true : false;

        String binaryPath = findWritableBinaryPath();

        String architecture = TestkubeDetectors.detectArchitecture();
        String system = TestkubeDetectors.detectSystem();

        TestkubeDetectors.detectKubectl(isCloudMode);
        var isTestkubeInstalled = TestkubeDetectors.detectTestkubeCLI(channel, version);

        if (!isTestkubeInstalled) {
            // If forced version is specified
            if (version != null) {
                TestkubeLogger.println("Forcing \"" + version.replaceFirst("^v", "") + "\" version...");
            }

            var versionToInstall = version != null ? version : TestkubeDetectors.detectTestkubeVersion(channel);

            installTestkubeCLI(versionToInstall, system, architecture, binaryPath);

        }

        configureTestkubeContext(isCloudMode);
    }

    private static String findWritableBinaryPath() throws Exception {
        List<String> preferredPaths = Arrays.asList("/usr/local/bin", "/usr/bin");

        String pathEnv = System.getenv("PATH");
        if (pathEnv == null || pathEnv.isEmpty()) {
            throw new IllegalStateException("PATH environment variable is not set.");
        }

        List<String> detectedPaths = Arrays.stream(pathEnv.split(File.pathSeparator))
                .filter(path -> !path.isEmpty())
                .sorted((a, b) -> Integer.compare(a.length(), b.length()))
                .collect(Collectors.toList());

        List<String> writablePaths = detectedPaths.stream()
                .filter(path -> Files.isWritable(Paths.get(path)))
                .collect(Collectors.toList());

        return preferredPaths.stream()
                .filter(writablePaths::contains)
                .findFirst()
                .orElseGet(() -> writablePaths.isEmpty() ? null : writablePaths.get(0));
    }

    private static void installTestkubeCLI(String version, String system, String architecture, String binaryDirPath)
            throws Exception {
        String artifactUrl = String.format(
                "https://github.com/kubeshop/testkube/releases/download/v%s/testkube_%s_%s_%s.tar.gz",
                URLEncoder.encode(version, StandardCharsets.UTF_8),
                URLEncoder.encode(version, StandardCharsets.UTF_8),
                URLEncoder.encode(system, StandardCharsets.UTF_8),
                URLEncoder.encode(architecture, StandardCharsets.UTF_8));

        System.out.println("Downloading the artifact from \"" + artifactUrl + "\"...");

        // Download the tar.gz file
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(artifactUrl))
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // Extract the tar.gz file
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(
                new GzipCompressorInputStream(new BufferedInputStream(response.body())))) {
            Files.createDirectories(Paths.get(binaryDirPath));
            Path outputPath = Paths.get(binaryDirPath, "kubectl-testkube");
            Files.copy(tarInput, outputPath);
            System.out.println("Extracted CLI to " + outputPath);

            // Create symbolic links
            Files.createSymbolicLink(Paths.get(binaryDirPath, "testkube"), outputPath);
            System.out.println("Linked CLI as " + Paths.get(binaryDirPath, "testkube"));

            Files.createSymbolicLink(Paths.get(binaryDirPath, "tk"), outputPath);
            System.out.println("Linked CLI as " + Paths.get(binaryDirPath, "tk"));
        } catch (Exception e) {
            throw new IOException("Failed to download or extract the artifact.", e);
        }
    }

    private void configureTestkubeContext(Boolean isCloudMode) throws Exception {
        List<String> command = new ArrayList<>();
        command.add("testkube"); // Command
        command.add("set");
        command.add("context");

        if (!isCloudMode) {
            // kubectl mode
            command.add("--kubeconfig");
            command.add("--namespace");
            command.add(namespace);
        } else {
            // Cloud mode
            command.add("--api-key");
            command.add(token);
            command.add("--cloud-root-domain");
            command.add(url);
            command.add("--org");
            command.add(organization);
            command.add("--env");
            command.add(environment);
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Failed to configure Testkube context with exit code: " + exitCode);
        }
    }
}
