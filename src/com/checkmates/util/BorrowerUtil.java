package com.checkmates.util;

import com.checkmates.model.Borrower;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.json.JSONObject;

public class BorrowerUtil {

    private static final String PHP_URL = "http://cm8tes.com/CS4347_Project_Folder/addBorrower.php";

    /** Add a new borrower via PHP endpoint */
    public boolean addBorrower(Borrower b) {
        // Validate required fields
        if (b.getName() == null || b.getName().isEmpty()) {
            showError("Name is required.");
            return false;
        }
        if (b.getSsn() == null || b.getSsn().isEmpty()) {
            showError("SSN is required.");
            return false;
        }
        if (b.getAddress() == null || b.getAddress().isEmpty()) {
            showError("Address is required.");
            return false;
        }

        // Generate card_id (PHP will also generate if not provided, but we do it here)
        String newCardId = generateNewCardId();
        b.setCardId(newCardId);

        // Send to PHP endpoint
        try {
            // Create JSON payload
            JSONObject jsonData = new JSONObject();
            jsonData.put("card_id", b.getCardId());
            jsonData.put("ssn", b.getSsn());
            jsonData.put("name", b.getName());
            jsonData.put("address", b.getAddress());
            jsonData.put("phone", b.getPhone() != null ? b.getPhone() : "");

            // Send POST request
            String response = sendJsonPost(PHP_URL, jsonData.toString());

            // Parse response
            try {
                JSONObject jsonResponse = new JSONObject(response);
                String status = jsonResponse.optString("status", "error");
                String message = jsonResponse.optString("message", "Unknown error");

                if ("success".equalsIgnoreCase(status)) {
                    // Update card_id if PHP generated a different one
                    if (jsonResponse.has("card_id")) {
                        b.setCardId(jsonResponse.getString("card_id"));
                    }
                    showSuccess("Borrower added successfully!\nCard ID: " + b.getCardId());
                    return true;
                } else {
                    showError(message);
                    return false;
                }
            } catch (org.json.JSONException e) {
                // Response is not valid JSON - show what we got
                System.err.println("Invalid JSON response: " + response);
                showError("Server returned invalid response. Check console for details.\nResponse: " + 
                    (response.length() > 200 ? response.substring(0, 200) + "..." : response));
                return false;
            }

        } catch (Exception e) {
            e.printStackTrace();
            String errorMsg = "Failed to connect to server: " + e.getMessage();
            if (e.getMessage().contains("JSONObject")) {
                errorMsg += "\n\nServer may be returning HTML error page instead of JSON.\nCheck PHP file on IONOS.";
            }
            showError(errorMsg);
            return false;
        }
    }

    /** Generate new card_id (fallback - PHP also generates if needed) */
    private String generateNewCardId() {
        // Default starting ID - PHP will handle actual generation from database
        return "10001";
    }

    /** Send JSON POST request to PHP endpoint */
    private String sendJsonPost(String urlString, String jsonData) throws Exception {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Content-Length", String.valueOf(jsonData.length()));

        // Send JSON data
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        // Read response
        int responseCode = conn.getResponseCode();
        InputStream inputStream = (responseCode >= 200 && responseCode < 300) 
            ? conn.getInputStream() 
            : conn.getErrorStream();

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        conn.disconnect();
        String responseStr = response.toString().trim();
        
        // Debug: Print response for troubleshooting
        System.out.println("PHP Response Code: " + responseCode);
        System.out.println("PHP Response: " + responseStr);
        
        // Check for HTTP error codes
        if (responseCode >= 400) {
            if (responseStr.isEmpty()) {
                throw new Exception("Server returned HTTP " + responseCode + " with empty response. Check PHP file on IONOS server.");
            }
            // Try to parse error JSON
            try {
                JSONObject errorJson = new JSONObject(responseStr);
                String errorMsg = errorJson.optString("message", "Server error");
                throw new Exception("Server error (" + responseCode + "): " + errorMsg);
            } catch (org.json.JSONException e) {
                throw new Exception("Server returned HTTP " + responseCode + ". Response: " + 
                    (responseStr.length() > 200 ? responseStr.substring(0, 200) + "..." : responseStr));
            }
        }
        
        // Check if response is empty or invalid
        if (responseStr.isEmpty()) {
            throw new Exception("Empty response from server. Check PHP file on IONOS server.");
        }
        
        // Check if response starts with JSON
        if (!responseStr.startsWith("{") && !responseStr.startsWith("[")) {
            throw new Exception("Invalid JSON response. Server returned: " + responseStr.substring(0, Math.min(100, responseStr.length())));
        }
        
        return responseStr;
    }

    /** Show error message on Swing thread */
    private void showError(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, 
                message,
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        });
    }

    /** Show success message on Swing thread */
    private void showSuccess(String message) {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(null, 
                message,
                "Success", 
                JOptionPane.INFORMATION_MESSAGE);
        });
    }
}
