package com.example.llandaffcampusapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * Base activity class that applies user settings to each activity.
 * All activities should extend this class to ensure consistent settings application.
 */
public abstract class BaseActivity extends AppCompatActivity {
    
    private static final String PREF_NAME = "LlandaffCampusSettings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_TEXT_SIZE = "text_size";
    private static final String LANG_ENGLISH = "en";
    private static final String TEXT_SIZE_NORMAL = "normal";
    private static final String TEXT_SIZE_LARGE = "large";
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Apply settings before calling super.onCreate to ensure they're applied to this activity
        applySettings();
        super.onCreate(savedInstanceState);
    }
    
    @Override
    protected void attachBaseContext(Context newBase) {
        // Apply settings to the base context
        SharedPreferences preferences = newBase.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Get saved settings
        String language = preferences.getString(PREF_LANGUAGE, LANG_ENGLISH);
        String textSize = preferences.getString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL);
        
        // Create configuration with these settings
        Configuration config = new Configuration(newBase.getResources().getConfiguration());
        config.setLocale(new Locale(language));
        
        if (TEXT_SIZE_LARGE.equals(textSize)) {
            config.fontScale = 1.3f;
        } else {
            config.fontScale = 1.0f;
        }
        
        // Create and use a context with these settings
        Context context = newBase.createConfigurationContext(config);
        super.attachBaseContext(context);
    }
    
    /**
     * Applies saved settings to this activity
     */
    private void applySettings() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        // Apply language setting
        String language = preferences.getString(PREF_LANGUAGE, LANG_ENGLISH);
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        
        // Apply text size
        String textSize = preferences.getString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL);
        if (TEXT_SIZE_LARGE.equals(textSize)) {
            config.fontScale = 1.3f;
        } else {
            config.fontScale = 1.0f;
        }
        
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
        createConfigurationContext(config);
    }
}
