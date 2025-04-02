package com.example.llandaffcampusapp;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Locale;

public class CampusApp extends Application {
    private static final String TAG = "CampusApp";
    private static final String USERS_COLLECTION = "users";
    private static final String SETTINGS_COLLECTION = "settings";
    
    private static final String PREF_NAME = "LlandaffCampusSettings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_TEXT_SIZE = "text_size";
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    private static final String LANG_ENGLISH = "en";
    private static final String TEXT_SIZE_NORMAL = "normal";
    private static final String TEXT_SIZE_LARGE = "large";
    
    // Firebase components
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;

    @Override
    public void onCreate() {
        super.onCreate();
        
        try {
            //init firebase
            FirebaseApp.initializeApp(this);
            mAuth = FirebaseAuth.getInstance();
            mFirestore = FirebaseFirestore.getInstance();
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }
        org.osmdroid.config.Configuration.getInstance().setUserAgentValue(getPackageName());
        
        //apply user's settings
        applyAppSettings();
        
        //check for user settings in Firebase
        try {
            checkFirebaseUserSettings();
        } catch (Exception e) {
            Log.e(TAG, "Error checking Firebase settings", e);
        }
    }

    private void applyAppSettings() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        //apply language setting
        String savedLanguage = preferences.getString(PREF_LANGUAGE, LANG_ENGLISH);
        setAppLanguage(savedLanguage);
        
        //apply text size setting
        String savedTextSize = preferences.getString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL);
        applyTextSize(savedTextSize);
    }
    
    private void checkFirebaseUserSettings() {
        SharedPreferences preferences = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(PREF_IS_LOGGED_IN, false);
        
        if (isLoggedIn && mAuth != null && mFirestore != null) {
            FirebaseUser currentUser = mAuth.getCurrentUser();
            if (currentUser != null) {
                String userId = currentUser.getUid();
                
                //check for settings in Firestore
                mFirestore.collection(USERS_COLLECTION)
                        .document(userId)
                        .collection(SETTINGS_COLLECTION)
                        .document("user_preferences")
                        .get()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                DocumentSnapshot document = task.getResult();
                                if (document.exists()) {
                                    //settings found -> apply them
                                    String language = document.getString(PREF_LANGUAGE);
                                    String textSize = document.getString(PREF_TEXT_SIZE);
                                    
                                    boolean needsRecreate = false;
                                    
                                    if (language != null && !language.equals(preferences.getString(PREF_LANGUAGE, LANG_ENGLISH))) {
                                        preferences.edit().putString(PREF_LANGUAGE, language).apply();
                                        setAppLanguage(language);
                                        needsRecreate = true;
                                    }
                                    
                                    if (textSize != null && !textSize.equals(preferences.getString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL))) {
                                        preferences.edit().putString(PREF_TEXT_SIZE, textSize).apply();
                                        applyTextSize(textSize);
                                        needsRecreate = true;
                                    }
                                    
                                    Log.d(TAG, "Applied settings from Firebase: " + language + ", " + textSize);
                                }
                            } else {
                                Log.w(TAG, "Error getting user settings", task.getException());
                            }
                        });
            }
        }
    }

    private void setAppLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        createConfigurationContext(config);
    }

    private void applyTextSize(String textSize) {
        //get current config
        Resources resources = getResources();
        Configuration config = resources.getConfiguration();

        //apply text size scale
        if (TEXT_SIZE_LARGE.equals(textSize)) {
            config.fontScale = 1.3f; // BIGGER text
        } else {
            config.fontScale = 1.0f; // smaller text
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
        createConfigurationContext(config);
    }
}