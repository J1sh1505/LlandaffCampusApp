package com.example.llandaffcampusapp1;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class CampusApp extends Application {
    private static final String PREF_NAME = "LlandaffCampusSettings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_TEXT_SIZE = "text_size";
    private static final String LANG_ENGLISH = "en";
    private static final String TEXT_SIZE_NORMAL = "normal";
    private static final String TEXT_SIZE_LARGE = "large";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Set the user agent to your app's package name or another unique identifier.
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());
        
        // Apply saved settings
        applyAppSettings();
    }

    private void applyAppSettings() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Apply language setting
        String savedLanguage = preferences.getString(PREF_LANGUAGE, LANG_ENGLISH);
        setAppLanguage(savedLanguage);
        
        // Apply text size setting
        String savedTextSize = preferences.getString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL);
        applyTextSize(savedTextSize);
    }

    private void setAppLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private void applyTextSize(String textSize) {
        // Get current configuration
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();

        // Apply text size scale
        if (TEXT_SIZE_LARGE.equals(textSize)) {
            config.fontScale = 1.3f; // Larger text
        } else {
            config.fontScale = 1.0f; // Normal text size
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}