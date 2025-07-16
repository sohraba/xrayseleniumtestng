package util;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class XrayUploader {

    public static void main(String[] args) {
        try {
            // API endpoint
            String url = "http://localhost:8080/rest/raven/2.0/import/execution";

            // Your Jira credentials
            String username = "sohraba";
            String password = "Sohrab@786";
            // Read JSON content from xray-result.json in project folder
            String payload = Files.readString(Paths.get("xray-result.json"));


//            // JSON payload
//            String payload = "{\n" +
//                    "                  \"testExecutionKey\": \"TES-6\",\n" +
//                    "                  \"tests\": [\n" +
//                    "                    { \"testKey\": \"TES-3\", \"status\": \"FAIL\" },\n" +
//                    "                    { \"testKey\": \"TES-4\", \"status\": \"FAIL\" }\n" +
//                    "                  ]\n" +
//                    "                }";

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
}
