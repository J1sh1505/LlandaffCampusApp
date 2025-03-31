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
    protected void attachBaseContext(Context base) {
        //apply settings before app context is created
        SharedPreferences preferences = base.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);

        //get saved settings
        String savedLanguage = preferences.getString(PREF_LANGUAGE, LANG_ENGLISH);
        String savedTextSize = preferences.getString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL);

        //apply settings to context
        Context updatedContext = updateBaseContextSettings(base, savedLanguage, savedTextSize);
        
        super.attachBaseContext(updatedContext);
    }

    private Context updateBaseContextSettings(Context context, String languageCode, String textSize) {
        //create locale from language code
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        //create config
        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.setLocale(locale);

        //apply text size
        if (TEXT_SIZE_LARGE.equals(textSize)) {
            configuration.fontScale = 1.3f;
        } else {
            configuration.fontScale = 1.0f;
        }

        //apply config to context
        return context.createConfigurationContext(configuration);


    }

    @Override
    public void onCreate() {
        super.onCreate();
        
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());
    }

    public void applyAppSettings() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        //apply language setting
        String savedLanguage = preferences.getString(PREF_LANGUAGE, LANG_ENGLISH);
        String savedTextSize = preferences.getString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL);

        //apply settings and update application context
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();

        //set locale
        Locale locale = new Locale(savedLanguage);
        Locale.setDefault(locale);
        config.setLocale(locale);

        //set font scale
        if (TEXT_SIZE_LARGE.equals(savedTextSize)) {
            config.fontScale = 1.3f;
        } else {
            config.fontScale = 1.0f;
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }
}