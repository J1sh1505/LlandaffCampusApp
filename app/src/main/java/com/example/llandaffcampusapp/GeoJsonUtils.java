package com.example.llandaffcampusapp;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

// This file reads GeoJSON files from assets into JSONObject for map rendering
public class GeoJsonUtils {
    private static final String TAG = "GeoJsonUtils";
    
    /**
     * Loads a GeoJSON file from assets and returns it as a JSONObject
     * @param context Application context
     * @param filename Name of the file in assets directory
     * @return JSONObject containing the GeoJSON data, or null if file not found or invalid
     */
    public static JSONObject loadGeoJsonFromAsset(Context context, String filename) {
        try {
            InputStream is = context.getAssets().open(filename);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);
            return new JSONObject(json);
        } catch (FileNotFoundException e) {
            Log.w(TAG, "GeoJSON file not found: " + filename);
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error loading GeoJSON: " + filename, e);
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Checks if a GeoJSON file exists in assets
     * @param context Application context
     * @param filename Name of the file in assets directory
     * @return true if file exists, false otherwise
     */
    public static boolean fileExists(Context context, String filename) {
        try {
            InputStream is = context.getAssets().open(filename);
            is.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
