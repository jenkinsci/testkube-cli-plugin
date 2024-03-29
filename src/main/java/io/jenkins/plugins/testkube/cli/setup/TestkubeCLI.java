package io.jenkins.plugins.testkube.cli.setup;

import hudson.EnvVars;
import hudson.util.Secret;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public class TestkubeCLI {
    private EnvVars envVars;
    private String version;
    private String channel;
    private String namespace;
    private String url;
    private String organization;
    private String environment;
    private Secret apiToken;
    private Boolean debug;

    public TestkubeCLI(PrintStream logger, EnvVars envVars) {
        TestkubeLogger.init(logger);
        this.envVars = envVars;
        this.version = getEnvVar("TK_VERSION", "TESTKUBE_VERSION");
        this.channel = getEnvVar("TK_CHANNEL", "TESTKUBE_CHANNEL");
        this.namespace = getEnvVar("TK_NAMESPACE", "TESTKUBE_NAMESPACE");
        this.url = getEnvVar("TK_URL", "TESTKUBE_URL");
        this.organization = getEnvVar("TK_ORG", "TESTKUBE_ORG");
        this.environment = getEnvVar("TK_ENV", "TESTKUBE_ENV");
        this.apiToken = Secret.fromString(getEnvVar("TK_API_TOKEN", "TESTKUBE_API_TOKEN"));
        String debugValue = envVars.get("TK_DEBUG");
        this.debug = debugValue != null && !debugValue.isEmpty();
    }

    public TestkubeCLI(
            PrintStream logger,
            EnvVars envVars,
            String version,
            String channel,
            String namespace,
            String url,
            String organization,
            String environment,
            String apiToken) {
        TestkubeLogger.init(logger);
        this.envVars = envVars;
        this.channel = channel;
        this.namespace = namespace;
        this.url = url;
        this.version = version;
        this.organization = organization;
        this.environment = environment;
        this.apiToken = Secret.fromString(apiToken);
    }

    private String getEnvVar(String tkKey, String testkubeKey) {
        String value = envVars.get(tkKey);
        if (value == null) {
            value = envVars.get(testkubeKey);
        }
        return value;
    }

    private void setDefaults() {
        envVars.put("NO_COLOR", "1");
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

    public boolean setup() {
        try {
            peformSetup();
            return true;
        } catch (Exception e) {
            TestkubeLogger.println("Error during setup: " + e.getMessage());
            if (debug) {
                e.printStackTrace(TestkubeLogger.getPrintStream());
            }
            return false;
        }
    }

    private void checkEnvironmentVariables() throws Exception {
        List<String> missingVariables = new ArrayList<>();

        if (organization == null) {
            missingVariables.add("organization");
        }

        if (environment == null) {
            missingVariables.add("environment");
        }

        if (apiToken == null) {
            missingVariables.add("apiToken");
        }

        if (!missingVariables.isEmpty()) {
            throw new Exception(
                    "The following arguments are missing: " + String.join(", ", missingVariables)
                            + ". If you want to run in Cloud Mode, please provide these arguments directly or using their corresponding environment variables.");
        } else {
            TestkubeLogger.println("Running in cloud mode...");
        }
    }

    private void peformSetup() throws Exception {
        setDefaults();

        Boolean isCloudMode = (organization != null || environment != null || apiToken != null) ? true : false;

        checkEnvironmentVariables();

        String binaryPath = findWritableBinaryPath();
        TestkubeLogger.println("Binary path: " + binaryPath);

        String architecture = TestkubeDetectors.detectArchitecture();
        String system = TestkubeDetectors.detectSystem();

        TestkubeDetectors.detectKubectl(isCloudMode);

        var installedTestkubeVersion = TestkubeDetectors.detectTestkubeCLI(channel, version);
        Boolean isInstalled = installedTestkubeVersion != null && !installedTestkubeVersion.isEmpty();
        String versionToInstall = null;

        if (!isInstalled) {
            versionToInstall = version != null ? version : TestkubeDetectors.detectTestkubeVersion(channel);
            TestkubeLogger.println("Installing \"" + versionToInstall + "\" version...");
        } else if (installedTestkubeVersion != null) {
            TestkubeLogger.println("Currently installed version: " + installedTestkubeVersion);
            if (version != null && !installedTestkubeVersion.equals(version)) {
                TestkubeLogger.println("Force install \"" + version + "\" version...");
                versionToInstall = version;
            }
        }

        if (versionToInstall != null) {
            installCLI(envVars, versionToInstall, system, architecture, binaryPath);
        }

        configureContext(isCloudMode);
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

    private static void installCLI(
            EnvVars envVars, String version, String system, String architecture, String binaryDirPath)
            throws Exception {
        String artifactUrl = String.format(
                "https://github.com/kubeshop/testkube/releases/download/v%s/testkube_%s_%s_%s.tar.gz",
                URLEncoder.encode(version, StandardCharsets.UTF_8),
                URLEncoder.encode(version, StandardCharsets.UTF_8),
                URLEncoder.encode(system, StandardCharsets.UTF_8),
                URLEncoder.encode(architecture, StandardCharsets.UTF_8));

        TestkubeLogger.println("Downloading the artifact from \"" + artifactUrl + "\"...");

        // Download the tar.gz file
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request =
                HttpRequest.newBuilder().uri(URI.create(artifactUrl)).build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        // Check response status code
        if (response.statusCode() != 200) {
            throw new IOException("Failed to download the Testkube CLI. HTTP status: " + response.statusCode());
        }

        // Save the downloaded file to a temporary file
        Path tempArchivePath = Paths.get(binaryDirPath, "tempTestkubeArchive.tar.gz");
        Files.copy(response.body(), tempArchivePath, StandardCopyOption.REPLACE_EXISTING);

        Path outputPath = Paths.get(binaryDirPath, "kubectl-testkube");

        // Remove outputPath if it already exists
        try {
            Files.deleteIfExists(outputPath);
            Files.deleteIfExists(Paths.get(binaryDirPath, "testkube"));
            Files.deleteIfExists(Paths.get(binaryDirPath, "tk"));
        } catch (Exception e) {
            throw new IOException("Failed to delete the existing testkube CLI.", e);
        }

        // Extract the tar.gz file
        try (TarArchiveInputStream tarInput = new TarArchiveInputStream(new GzipCompressorInputStream(
                new BufferedInputStream(new FileInputStream(tempArchivePath.toFile()))))) {
            Files.createDirectories(Paths.get(binaryDirPath));
            TarArchiveEntry entry;
            while ((entry = (TarArchiveEntry) tarInput.getNextTarEntry()) != null) {
                // Create a path for the entry
                Path entryPath = Paths.get(binaryDirPath, entry.getName());

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Path parentDir = entryPath.getParent();
                    if (parentDir != null) {
                        Files.createDirectories(parentDir);
                    }
                    Files.copy(tarInput, entryPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }

            if ("Darwin".equals(system) || "Linux".equals(system)) {
                Set<PosixFilePermission> perms = PosixFilePermissions.fromString("rwxr--r--");
                Files.setPosixFilePermissions(outputPath, perms);
                TestkubeLogger.println("Set execute permissions for " + outputPath);
            }

            Files.createSymbolicLink(Paths.get(binaryDirPath, "testkube"), outputPath);
            TestkubeLogger.println("Linked CLI as " + Paths.get(binaryDirPath, "testkube"));

            Files.createSymbolicLink(Paths.get(binaryDirPath, "tk"), outputPath);
            TestkubeLogger.println("Linked CLI as " + Paths.get(binaryDirPath, "tk"));

        } catch (IOException e) {
            throw new IOException("Failed to download or extract the artifact.", e);
        } finally {
            // Clean up: Delete the temporary file
            Files.deleteIfExists(tempArchivePath);
        }
    }

    private void configureContext(Boolean isCloudMode) throws Exception {
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
            command.add(Secret.toString(apiToken));
            command.add("--org");
            command.add(organization);
            command.add("--env");
            command.add(environment);
            if (url != null) {
                command.add("--root-domain");
                command.add(url);
            }
        }

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.inheritIO();
        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new RuntimeException("Failed to configure Testkube context with exit code: " + exitCode);
        } else {
            TestkubeLogger.println("Context configured successfully.");
        }
    }
}
