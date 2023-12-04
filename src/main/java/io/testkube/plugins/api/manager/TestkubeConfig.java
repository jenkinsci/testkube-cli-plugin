package io.testkube.plugins.api.manager;

import java.util.List;

import org.jenkinsci.plugins.plaincredentials.StringCredentials;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;

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
        Jenkins jenkins = Jenkins.get();
        List<StringCredentials> credentialsList = CredentialsProvider.lookupCredentialsInItemGroup(
                StringCredentials.class,
                jenkins,
                ACL.SYSTEM2,
                null);

        getInstance().apiUrl = getInstance().apiUrl != null ? getInstance().apiUrl
                : getPlainTextFromCredentials("apiUrl", credentialsList);
        getInstance().apiToken = getInstance().apiToken != null ? getInstance().apiToken
                : getPlainTextFromCredentials("apiToken", credentialsList);
        getInstance().orgId = getInstance().orgId != null ? getInstance().orgId
                : getPlainTextFromCredentials("orgId", credentialsList);
        getInstance().envId = getInstance().envId != null ? getInstance().envId
                : getPlainTextFromCredentials("envId", credentialsList);
        getInstance().namespace = getInstance().namespace != null ? getInstance().namespace
                : getPlainTextFromCredentials("namespace", credentialsList, "testkube");
    }

    public static String getPlainTextFromCredentials(String credentialsId, List<StringCredentials> credentialsList) {
        return getPlainTextFromCredentials(credentialsId, credentialsList, null);
    }
    public static String getPlainTextFromCredentials(String credentialsId, List<StringCredentials> credentialsList, String defaultValue) {
        String uppercaseCredentialsId = "TESTKUBE_"
                + credentialsId.replaceAll("(\\p{Lower})(\\p{Upper})", "$1_$2").toUpperCase();

        StringCredentials credentials = CredentialsMatchers.firstOrNull(
                credentialsList,
                CredentialsMatchers.withId(uppercaseCredentialsId));

        if (credentials == null) {
            if (defaultValue != null) {
                return defaultValue;
            }
            TestkubeLogger.println("Missing " + credentialsId
                    + " argument. Please set a value for the argument or set a String Credential with the ID: "
                    + uppercaseCredentialsId);
            return null;
        }
        return credentials.getSecret().getPlainText();
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
                    "TestkubeConfig has not been initialized. Call TestkubeConfig.init() first.");
        }
    }
}