package io.jenkins.plugins.testkube.cli.setup;

import hudson.EnvVars;
import hudson.util.Secret;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public TestkubeCLI(PrintStream printStream, EnvVars envVars) {
        TestkubeLogger.setPrintStream(printStream);
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
        TestkubeLogger.setDebug(this.debug);
    }

    public TestkubeCLI(
            PrintStream printStream,
            EnvVars envVars,
            String version,
            String channel,
            String namespace,
            String url,
            String organization,
            String environment,
            String apiToken) {
        TestkubeLogger.setPrintStream(printStream);
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
        TestkubeLogger.debug("Setting default values...");
        envVars.put("NO_COLOR", "1");
        if (namespace == null) {
            namespace = "testkube";
            TestkubeLogger.debug("Using default namespace: " + namespace);
        }
        if (channel == null) {
            channel = "stable";
            TestkubeLogger.debug("Using default channel: " + channel);
        }
        if (url == null) {
            url = "testkube.io";
            TestkubeLogger.debug("Using default URL: " + url);
        }
        TestkubeLogger.debug("Environment variables after defaults: " + envVars);
    }

    public boolean setup() {
        try {
            peformSetup();
            return true;
        } catch (Exception e) {
            TestkubeLogger.error("Setup failed", e);
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

        if (isCloudMode) {
            checkEnvironmentVariables();
        }

        String binaryPath = findWritableBinaryPath();
        if (binaryPath == null) {
            throw new TestkubeException(
                    "Failed to find a writable directory to install the Testkube CLI.",
                    "No writable directory was detected in the Jenkins pipeline environment.",
                    Arrays.asList(
                            "Verify that common binary directories (/usr/local/bin, /usr/bin) are writable by the Jenkins user",
                            "Check directory permissions using 'ls -la' in your pipeline",
                            "Create a dedicated writable directory in the Jenkins home directory",
                            "Ensure the PATH environment variable includes writable directories"));
        }
        TestkubeLogger.println("Found writable path for installing Testkube's CLI: " + binaryPath);

        String architecture = TestkubeDetectors.detectArchitecture();
        String system = TestkubeDetectors.detectSystem();

        checkSystemCompatibility(system, architecture);

        try {
            TestkubeDetectors.detectKubectl(isCloudMode);
        } catch (Exception e) {
            throw new TestkubeException(
                    "Kubectl verification failed",
                    e.getMessage(),
                    Arrays.asList(
                            "Ensure kubectl is installed in your pipeline environment",
                            "Verify kubectl is available in the system PATH",
                            "Check if the Kubernetes cluster configuration is accessible",
                            "Run 'kubectl version' manually to verify the installation"));
        }

        var installedVersion = TestkubeDetectors.detectTestkubeCLI(channel, version);
        String versionToInstall = determineVersionToInstall(installedVersion);

        try {
            if (versionToInstall != null) {
                installCLI(envVars, versionToInstall, system, architecture, binaryPath);
            }
        } catch (IOException e) {
            throw new TestkubeException(
                    "Failed to install Testkube CLI",
                    e.getMessage(),
                    Arrays.asList(
                            "Check network connectivity to GitHub releases",
                            "Verify write permissions in the installation directory",
                            "Ensure sufficient disk space is available",
                            "Try manually downloading the release from GitHub"));
        }

        try {
            configureContext(isCloudMode);
        } catch (Exception e) {
            throw new TestkubeException(
                    "Context configuration failed",
                    e.getMessage(),
                    Arrays.asList(
                            "Verify your API token is valid and not expired",
                            "Check if the organization and environment IDs are correct",
                            "Ensure the Testkube API endpoint is accessible",
                            "Verify network connectivity to the Testkube service"));
        }
    }

    private String determineVersionToInstall(String installedVersion) throws TestkubeException {
        try {
            if (installedVersion == null) {
                String detectedVersion = version;
                if (detectedVersion == null) {
                    try {
                        detectedVersion = TestkubeDetectors.detectTestkubeVersion(channel);
                    } catch (Exception e) {
                        throw new TestkubeException("Failed to detect Testkube version", e.getMessage());
                    }
                }
                return detectedVersion;
            }

            if (version != null && !installedVersion.equals(version)) {
                TestkubeLogger.println("Force install \"" + version + "\" version...");
                return version;
            }

            return null;
        } catch (Exception e) {
            if (e instanceof TestkubeException) {
                throw (TestkubeException) e;
            }
            throw new TestkubeException(
                    "Version detection failed",
                    e.getMessage(),
                    Arrays.asList(
                            "Try specifying a version explicitly using TK_VERSION",
                            "Check if the requested version exists",
                            "Verify the version format is correct"));
        }
    }

    private void checkSystemCompatibility(String system, String architecture) throws TestkubeException {
        List<String> supportedSystems = Arrays.asList("Linux", "Darwin", "Windows");
        List<String> supportedArchitectures = Arrays.asList("x86_64", "arm64", "i386");

        if (!supportedSystems.contains(system)) {
            throw new TestkubeException(
                    "Unsupported operating system", "Operating system '" + system + "' is not supported");
        }

        if (!supportedArchitectures.contains(architecture)) {
            throw new TestkubeException(
                    "Unsupported system architecture", "Architecture '" + architecture + "' is not supported");
        }
    }

    private static String findWritableBinaryPath() {
        TestkubeLogger.debug("Searching for writable binary path...");

        List<String> commonBinaryPaths = Arrays.asList("/usr/local/bin", "/usr/bin", "/opt/bin", "/bin");

        for (String path : commonBinaryPaths) {
            if (isWritablePath(path)) {
                TestkubeLogger.debug("Found writable common binary path: " + path);
                return path;
            }
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && isWritablePath(userHome)) {
            TestkubeLogger.debug("Using writable user home directory: " + userHome);
            return userHome;
        }

        String pathEnv = System.getenv("PATH");
        if (pathEnv != null && !pathEnv.isEmpty()) {
            for (String path : pathEnv.split(File.pathSeparator)) {
                if (!path.isEmpty() && isWritablePath(path)) {
                    TestkubeLogger.debug("Found writable PATH directory: " + path);
                    return path;
                }
            }
        }

        return null;
    }

    private static boolean isWritablePath(String path) {
        try {
            Path directoryPath = Paths.get(path);
            return Files.exists(directoryPath) && Files.isDirectory(directoryPath) && Files.isWritable(directoryPath);
        } catch (Exception e) {
            TestkubeLogger.debug("Error while checking if path " + path + " is writable: " + e.getMessage());
            return false;
        }
    }

    private static void installCLI(
            EnvVars envVars, String version, String system, String architecture, String binaryDirPath)
            throws Exception {
        TestkubeLogger.debug("Starting CLI installation...");
        TestkubeLogger.debug(String.format(
                "Installation parameters: version=%s, system=%s, architecture=%s, binaryDirPath=%s",
                version, system, architecture, binaryDirPath));

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
            command.add("--org-id");
            command.add(organization);
            command.add("--env-id");
            command.add(environment);
            if (url != null) {
                command.add("--root-domain");
                command.add(url);
            }
        }

        TestkubeLogger.debug("Executing command: " + String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);

        // Set NO_COLOR environment variable to disable color output in the process
        processBuilder.environment().put("NO_COLOR", "1");

        // Create the process without inheritIO
        Process process = processBuilder.start();

        // Create separate threads to handle stdout and stderr
        Thread outputThread = new Thread(() -> {
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    TestkubeLogger.println("[CLI] " + line);
                }
            } catch (IOException e) {
                TestkubeLogger.error("Error reading CLI output", e);
            }
        });

        Thread errorThread = new Thread(() -> {
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    TestkubeLogger.println("[CLI] " + line);
                }
            } catch (IOException e) {
                TestkubeLogger.error("Error reading CLI error stream", e);
            }
        });

        // Start both threads
        outputThread.start();
        errorThread.start();

        // Wait for the process to complete
        int exitCode = process.waitFor();

        // Wait for the output threads to finish
        outputThread.join();
        errorThread.join();

        if (exitCode != 0) {
            throw new RuntimeException("Failed to configure Testkube context with exit code: " + exitCode);
        } else {
            TestkubeLogger.println("[CLI] Context configured successfully.");
        }
    }
}
