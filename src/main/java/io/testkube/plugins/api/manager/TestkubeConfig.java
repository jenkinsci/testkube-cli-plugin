package io.testkube.plugins.api.manager;

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

    public static void init(String orgId, String apiUrl, String envId, String apiToken, String namespace) {
        Utils.checkNotNull("orgId", orgId);
        Utils.checkNotNull("apiUrl", apiUrl);
        Utils.checkNotNull("envId", envId);
        Utils.checkNotNull("apiToken", apiToken);

        getInstance().orgId = orgId;
        getInstance().apiUrl = apiUrl;
        getInstance().envId = envId;
        getInstance().apiToken = apiToken;
        getInstance().namespace = namespace;
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

    private static void checkInitialized() {
        if (getInstance().orgId == null || getInstance().apiUrl == null || getInstance().envId == null
                || getInstance().apiToken == null) {
            throw new IllegalStateException(
                    "TestkubeConfig has not been initialized. Call TestkubeConfig.init() first.");
        }
    }
}