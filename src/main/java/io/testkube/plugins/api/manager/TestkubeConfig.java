package io.testkube.plugins.api.manager;

import java.util.List;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;

import hudson.EnvVars;
import hudson.security.ACL;
import jenkins.model.Jenkins;

public class TestkubeConfig {
    private static TestkubeConfig instance = null;

    private String orgId;
    private String apiUrl;
    private String envId;
    private String apiToken;
    private String namespace;

    private TestkubeConfig() {
        // private constructor to prevent instantiation
    }

    private static TestkubeConfig getInstance() {
        if (instance == null) {
            instance = new TestkubeConfig();
        }
        return instance;
    }

    public static void init() {
        init(new EnvVars());
    }

    public static void init(EnvVars envVars) {
        Jenkins jenkins = Jenkins.get();
        List<StringCredentials> credentialsList = CredentialsProvider.lookupCredentialsInItemGroup(
                StringCredentials.class,
                jenkins,
                ACL.SYSTEM2,
                null);

        if (getInstance().apiUrl == null) {
            getInstance().apiUrl = getArgValue("apiUrl", envVars, credentialsList, "https://api.testkube.io");
        }
        if (getInstance().apiToken == null) {
            getInstance().apiToken = getArgValue("apiToken", envVars, credentialsList);
        }
        if (getInstance().orgId == null) {
            getInstance().orgId = getArgValue("orgId", envVars, credentialsList);
        }
        if (getInstance().envId == null) {
            getInstance().envId = getArgValue("envId", envVars, credentialsList);
        }
        if (getInstance().namespace == null) {
            getInstance().namespace = getArgValue("namespace", envVars, credentialsList, "testkube");
        }

        getInstance().apiUrl = getArgValue("apiUrl", envVars, credentialsList);
    }

    private static String getArgValue(String argName, EnvVars envVars, List<StringCredentials> credentialsList) {
        return getArgValue(argName, envVars, credentialsList, null);
    }

    private static String getArgValue(String argName, EnvVars envVars, List<StringCredentials> credentialsList,
            String defaultValue) {
        String uppercaseArgName = "TESTKUBE_"
                + argName.replaceAll("(\\p{Lower})(\\p{Upper})", "$1_$2").toUpperCase();

        String argValue = null;
        argValue = envVars.get(uppercaseArgName);

        if (argValue == null) {
            StringCredentials credentials = CredentialsMatchers.firstOrNull(
                    credentialsList,
                    CredentialsMatchers.withId(uppercaseArgName));
            if (credentials != null) {
                argValue = credentials.getSecret().getPlainText();
            }
        }

        if (argValue == null) {
            argValue = defaultValue;
        }

        if (argValue == null) {
            TestkubeLogger.println("Missing " + argName
                    + " argument. Please set a value for the argument or alternatively set an environment variable or a Jenkins credential with the identifier "
                    + uppercaseArgName);
        }

        return argValue;

    }

    public static String getOrgId() {
        checkInitialized();
        return getInstance().orgId;
    }

    public static String getApiUrl() {
        checkInitialized();
        return getInstance().apiUrl;
    }

    public static String getEnvId() {
        checkInitialized();
        return getInstance().envId;
    }

    public static String getApiToken() {
        checkInitialized();
        return getInstance().apiToken;
    }

    public static String getNamespace() {
        checkInitialized();
        return getInstance().namespace != null ? getInstance().namespace : "testkube";
    }

    public static void setOrgId(String orgId) {
        getInstance().orgId = orgId;
    }

    public static void setApiUrl(String apiUrl) {
        getInstance().apiUrl = apiUrl;
    }

    public static void setEnvId(String envId) {
        getInstance().envId = envId;
    }

    public static void setApiToken(String apiToken) {
        getInstance().apiToken = apiToken;
    }

    public static void setNamespace(String namespace) {
        getInstance().namespace = namespace;
    }

    private static void checkInitialized() {
        if (getInstance().orgId == null || getInstance().apiUrl == null || getInstance().envId == null
                || getInstance().apiToken == null) {
            throw new IllegalStateException(
                    "TestkubeConfig has not been initialized correctly, one of the following arguments not found: orgId, apiUrl, envId or apiToken.");
        }
    }
}