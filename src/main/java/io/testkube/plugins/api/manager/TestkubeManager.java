package io.testkube.plugins.api.manager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;

public class TestkubeManager {

    private static <T extends HttpUriRequest> T createRequest(Class<T> httpMethod, String urlPath) {
        String apiUrl = TestkubeConfig.getApiUrl();
        String orgId = TestkubeConfig.getOrgId();
        String envId = TestkubeConfig.getEnvId();
        String apiToken = TestkubeConfig.getApiToken();
        String baseUrl = apiUrl + "/organizations/" + orgId + "/environments/" + envId + "/agent";

        try {
            Constructor<T> constructor = httpMethod.getConstructor(String.class);
            var request = constructor.newInstance(baseUrl + urlPath);
            request.setHeader("Authorization", "Bearer " + apiToken);
            request.setHeader("Content-Type", "application/json");
            return request;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create request for path " + urlPath, e);
        }
    }

    private static <T extends HttpEntityEnclosingRequestBase> T createRequest(Class<T> httpMethod, String urlPath,
            String body) {
        T request = createRequest(httpMethod, urlPath);
        if (body != null) {
            try {
                StringEntity entity = new StringEntity(body);
                request.setEntity(entity);
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException("Failed to set request body", e);
            }
        }
        return request;
    }

    private static String run(TestkubeTestType testType, String testName) {
        String msgPrefix = "runTest(" + testName + ") ";
        String executionId = null;

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String namespace = TestkubeConfig.getNamespace();
            String runTestUrl = "/" + testType.getValue() + "/" + testName + "/executions";
            String jsonBody = "{\"namespace\": \"" + namespace + "\"}";
            var request = createRequest(HttpPost.class, runTestUrl, jsonBody);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                switch (statusCode) {
                    case 201:
                        HttpEntity entity = response.getEntity();
                        String result = EntityUtils.toString(entity);
                        JSONObject json = new JSONObject(result);
                        executionId = json.getString("id");
                        TestkubeLogger.println(msgPrefix + "Successfully created execution with id: " + executionId);
                        break;
                    case 400:
                        TestkubeLogger.println(msgPrefix + "Problem with request body.");
                        break;
                    case 404:
                        TestkubeLogger.println(msgPrefix + "Test not found.");
                        break;
                    case 500:
                        TestkubeLogger.println(msgPrefix + "Problem with test execution.");
                        break;
                    case 502:
                        TestkubeLogger.println(msgPrefix + "Problem with communicating with Kubernetes cluster.");
                        break;
                    default:
                        TestkubeLogger.println(msgPrefix + "Unexpected status code: " + statusCode);
                }
            }

        } catch (IOException e) {
            TestkubeLogger.println(msgPrefix + "Error: " + e.getMessage());
        }

        return executionId;
    }

    public static String runTest(String testName) {
        return run(TestkubeTestType.TEST, testName);
    }

    public static String runTestSuite(String testName) {
        return run(TestkubeTestType.TEST_SUITE, testName);
    }

    public static TestkubeTestResult waitForExecution(String testName, String executionId) {
        String msgPrefix = "waitForExecution(" + testName + ") ";
        String status = null;
        String previousStatus = null;
        String output = null;
        String errorMessage = null;
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            var request = createRequest(HttpGet.class, "/tests/" + testName + "/executions/" + executionId);
            TestkubeLogger.println(msgPrefix + "Request URI:" + request.getURI().toString());
            while (true) {
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    HttpEntity entity = response.getEntity();
                    String result = EntityUtils.toString(entity);
                    JSONObject json = new JSONObject(result);
                    // TODO: use the below code after the getExecutionsById API is fixed
                    // TestkubeLogger.println(result);
                    // JSONObject executionResultObject = json.getJSONObject("executionResult");
                    // output = executionResultObject.getString("output");
                    // errorMessage = executionResultObject.getString("errorMessage");
                    // status = executionResultObject.getString("status");
                    status = getStatusFromResults(json, executionId);
                    if (!status.equals(previousStatus)) {
                        TestkubeLogger.println(msgPrefix + "Execution Status: " + status);
                        previousStatus = status;
                    }
                    if (!status.equals("queued") && !status.equals("running")) {
                        TestkubeLogger.println(msgPrefix + "Execution finished with status: " + status);
                        break;
                    }
                    Thread.sleep(1000);
                } catch (JSONException e) {
                    TestkubeLogger.println(msgPrefix + "JSON Error: " + e.getMessage());
                    break;
                } catch (IOException e) {
                    TestkubeLogger.println(msgPrefix + "Error: " + e.getMessage());
                    break;
                } catch (InterruptedException e) {
                    TestkubeLogger.println(msgPrefix + "Interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (IOException e) {
            TestkubeLogger.println(msgPrefix + "Error: " + e.getMessage());
        }
        if (status == null) {
            return null;
        }
        return new TestkubeTestResult(executionId, status, output, errorMessage);
    }

    public static String getStatusFromResults(JSONObject json, String executionId) {
        JSONArray resultsArray = json.getJSONArray("results");
        for (int i = 0; i < resultsArray.length(); i++) {
            JSONObject executionResultObject = resultsArray.getJSONObject(i);
            if (executionResultObject.getString("id").equals(executionId)) {
                return executionResultObject.getString("status");
            }
        }
        return null;
    }

}
