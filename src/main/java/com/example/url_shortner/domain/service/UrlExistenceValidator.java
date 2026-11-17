package com.example.url_shortner.domain.service;

import java.net.HttpURLConnection;
import java.net.URL;

public class UrlExistenceValidator {

    public static boolean isUrlExists(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            huc.setRequestMethod("HEAD"); // Performs a lightweight connection test without downloading the full page
            int responseCode = huc.getResponseCode();
            return (responseCode >= 200 && responseCode < 400);
        } catch (Exception e) {
            return false; // Returns false if the URL is broken or malformed
        }
    }
}
