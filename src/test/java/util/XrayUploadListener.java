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
import org.xml.sax.SAXException;

public class XrayUploadListener implements ISuiteListener {

    @Override
    public void onFinish(ISuite suite) {
        System.out.println("I am in On finish");
        try {
            String testExecKey = "TES-5"; // Optional if using ?projectKey=XYZ
            String reportPath = "xray-result.json";
            File reportFile = new File(reportPath);
            // generateTestNGJson();

            if (!reportFile.exists()) {
                System.out.println("Report file not found: " + reportPath);
                return;
            }
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

    public void generateTestNGJson() throws ParserConfigurationException, IOException, SAXException {
        File xmlFile = new File("target/surefire-reports/testng-results.xml");

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(xmlFile);

        doc.getDocumentElement().normalize();

        JSONObject xrayJson = new JSONObject();
        xrayJson.put("testExecutionKey", "TES-5");

        JSONObject info = new JSONObject();
        info.put("summary", "Test Execution from TestNG");
        info.put("description", "Auto generated from TestNG XML");
        xrayJson.put("info", info);

        JSONArray tests = new JSONArray();

        NodeList testMethods = doc.getElementsByTagName("test-method");
        for (int i = 0; i < testMethods.getLength(); i++) {
            Element method = (Element) testMethods.item(i);

            String name = method.getAttribute("name");
            String status = method.getAttribute("status");
            String testKey = "TES-" + (i);  // Map your own test keys here

            JSONObject test = new JSONObject();
            test.put("testKey", testKey);

            // Map TestNG status to Xray status
            switch (status.toLowerCase()) {
                case "pass":
                    test.put("status", "PASSED");
                    break;
                case "fail":
                    test.put("status", "FAILED");
                    break;
                case "skip":
                    test.put("status", "SKIPPED");
                    break;
                default:
                    test.put("status", "FAILED");
            }

            tests.put(test);
        }

        xrayJson.put("tests", tests);

        try (FileWriter file = new FileWriter("xray-result.json")) {
            file.write(xrayJson.toString(2));  // pretty print
        }

        System.out.println("Xray JSON generated: xray-result.json");
    }

}
