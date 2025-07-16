package util;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import org.json.JSONArray;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
public class XrayJsonUploader {

        // Change these
        private static final String TESTNG_XML_FILE = "target\\surefire-reports\\testng-results.xml";
        private static final String TEST_EXECUTION_KEY = "TES-5";  // Your Xray test execution issue key
        private static final String XRAY_API_URL = "http://localhost:8080/rest/raven/1.0/import/execution"; // Xray Data Center / Server URL
        private static final String USERNAME = "sohraba";
        private static final String PASSWORD = "Sohrab@786";

        public static void main(String[] args) throws Exception {
            JSONObject xrayJson = convertTestngXmlToXrayJson(TESTNG_XML_FILE, TEST_EXECUTION_KEY);
            System.out.println("Uploading JSON to Xray:");
            System.out.println(xrayJson.toString(2));

            uploadJsonToXray(xrayJson, XRAY_API_URL, USERNAME, PASSWORD);
        }

        public static JSONObject convertTestngXmlToXrayJson(String xmlFilePath, String testExecutionKey) throws Exception {
            File xmlFile = new File(xmlFilePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();

            JSONArray testsArray = new JSONArray();

            NodeList testMethodNodes = doc.getElementsByTagName("test-method");
            for (int i = 0; i < testMethodNodes.getLength(); i++) {
                Element testMethod = (Element) testMethodNodes.item(i);

                // Skip config methods
                if ("true".equalsIgnoreCase(testMethod.getAttribute("is-config"))) {
                    continue;
                }

                String status = testMethod.getAttribute("status");
                String xrayStatus = mapStatusToXray(status);

                // Find "requirement" attribute for test key
                NodeList attrList = testMethod.getElementsByTagName("attribute");
                String testKey = null;
                for (int j = 0; j < attrList.getLength(); j++) {
                    Element attr = (Element) attrList.item(j);
                    if ("requirement".equalsIgnoreCase(attr.getAttribute("name"))) {
                        testKey = attr.getTextContent().trim();
                        break;
                    }
                }

                if (testKey != null && !testKey.isEmpty()) {
                    JSONObject testObj = new JSONObject();
                    testObj.put("testKey", testKey);
                    testObj.put("status", xrayStatus);
                    testsArray.put(testObj);
                }
            }

            JSONObject root = new JSONObject();
            root.put("testExecutionKey", testExecutionKey);
            root.put("tests", testsArray);

            return root;
        }

        private static String mapStatusToXray(String testngStatus) {
            switch (testngStatus.toUpperCase()) {
                case "PASS":
                    return "PASS";
                case "FAIL":
                    return "FAIL";
                case "SKIP":
                    return "TODO";
                default:
                    return "TODO";
            }
        }

        private static void uploadJsonToXray(JSONObject xrayJson, String apiUrl, String username, String password) throws Exception {
            System.out.println("Uploading JSON test results to Xray...");

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost post = new HttpPost(apiUrl);

                String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
                post.setHeader("Authorization", "Basic " + auth);
                post.setHeader("Content-Type", "application/json");

                StringEntity entity = new StringEntity(xrayJson.toString(), "UTF-8");
                post.setEntity(entity);

                HttpResponse response = client.execute(post);
                int status = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());

                System.out.println("Response Code: " + status);
                System.out.println("Response Body: " + responseBody);

                if (status != 200) {
                    System.out.println("❌ Upload failed. Check JSON content, status values, or Jira/Xray logs.");
                } else {
                    System.out.println("✅ Upload successful.");
                }
            }
        }
    }
