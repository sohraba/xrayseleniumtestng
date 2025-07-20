package util;

import org.testng.ISuite;
import org.testng.ISuiteListener;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.*;

import org.json.*;

public class XrayUploadListener implements ISuiteListener {

    @Override
    public void onFinish(ISuite suite) {
        System.out.println("I am in On finish");
        try {
            RepositoryParser repo = new RepositoryParser("./src/configs/object.properties");
            // Specify the input TestNG XML result file
            String testngXmlFile = "target\\surefire-reports\\testng-results.xml";  // Path to the TestNG XML file
            String testExecutionKey = repo.getBy("testExecutionKey");  // Your test execution key

            // Convert the TestNG XML to Xray JSON format
            JSONObject xrayJson = convertToXrayJson(testngXmlFile, testExecutionKey);

            // Write the generated Xray JSON to "xray-result.json"
            writeJsonToFile(xrayJson, "xray-result.json");
            uploadJsonToXray();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void uploadJsonToXray() throws IOException {
        try {
            // API endpoint
            String url = "http://localhost:8080/rest/raven/2.0/import/execution";

            // Your Jira credentials
            String username = "sohraba";
            String password = "Sohrab@786";
            // Read JSON content from xray-result.json in project folder
            String payload = Files.readString(Paths.get("xray-result.json"));

            // Encode credentials
            String auth = Base64.getEncoder().encodeToString((username + ":" + password).getBytes());

            // Open connection
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Basic " + auth);
            conn.setDoOutput(true);

            // Send payload
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes("UTF-8"));
            }

            // Read response
            int responseCode = conn.getResponseCode();
            System.out.println("Response Code: " + responseCode);

            InputStream is = responseCode < 400 ? conn.getInputStream() : conn.getErrorStream();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                String line;
                while ((line = br.readLine()) != null) {
                    System.out.println(line);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to extract Xray test IDs and statuses from the TestNG XML and generate Xray JSON
    public static JSONObject convertToXrayJson(String testngXmlFile, String testExecutionKey) throws Exception {
        // Parse the TestNG XML result file
        File xmlFile = new File(testngXmlFile);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        // Create a JSON object to hold the final result
        JSONObject xrayJson = new JSONObject();
        xrayJson.put("testExecutionKey", testExecutionKey);

        // Create a JSON array to hold all the test details
        JSONArray testsArray = new JSONArray();

        // Extract the <test-method> nodes from the XML
        NodeList testMethods = doc.getElementsByTagName("test-method");

        for (int i = 0; i < testMethods.getLength(); i++) {
            Node testMethodNode = testMethods.item(i);

            if (testMethodNode.getNodeType() == Node.ELEMENT_NODE) {
                Element testMethodElement = (Element) testMethodNode;

                // Extract the test status (PASS/FAIL)
                String status = testMethodElement.getAttribute("status");

                // Extract the <attributes> element to find the "test" key
                NodeList attributesList = testMethodElement.getElementsByTagName("attributes");
                for (int j = 0; j < attributesList.getLength(); j++) {
                    Element attributesElement = (Element) attributesList.item(j);

                    // Extract the <attribute> with name "test"
                    NodeList attributeList = attributesElement.getElementsByTagName("attribute");
                    for (int k = 0; k < attributeList.getLength(); k++) {
                        Element attributeElement = (Element) attributeList.item(k);
                        String attributeName = attributeElement.getAttribute("name");

                        // Check if the attribute name is "test", which contains the Xray test ID (e.g., TES-3)
                        if ("test".equals(attributeName)) {
                            // Trim the text content to remove any extra whitespace
                            String testID = attributeElement.getTextContent().trim();

                            // Create a JSON object for each test
                            JSONObject testJson = new JSONObject();
                            testJson.put("testKey", testID);  // Now it's properly trimmed
                            testJson.put("status", status);

                            // Add the test JSON object to the tests array
                            testsArray.put(testJson);
                        }
                    }
                }
            }
        }

        // Add the tests array to the main Xray JSON object
        xrayJson.put("tests", testsArray);

        return xrayJson;
    }

    // Method to write the Xray JSON result into a file
    public static void writeJsonToFile(JSONObject json, String fileName) throws IOException {
        // Create a FileWriter to write to the file
        try (FileWriter file = new FileWriter(fileName)) {
            file.write(json.toString(2));  // Pretty print the JSON with an indent of 2
        }
    }
}