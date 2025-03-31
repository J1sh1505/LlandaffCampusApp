package com.example.llandaffcampusapp1;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String PREF_NAME = "LlandaffCampusSettings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_TEXT_SIZE = "text_size";
    private static final String LANG_ENGLISH = "en";
    private static final String TEXT_SIZE_NORMAL = "normal";
    private static final String TEXT_SIZE_LARGE = "large";

    @Override
    protected void attachBaseContext(Context baseContext) {
        //get saved preferences
        SharedPreferences preferences = baseContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String savedLanguage = preferences.getString(PREF_LANGUAGE, LANG_ENGLISH);
        String savedTextSize = preferences.getString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL);

        //create config with prefs
        Configuration configuration = new Configuration(baseContext.getResources().getConfiguration());
        
        //set language
        Locale locale = new Locale(savedLanguage);
        Locale.setDefault(locale);
        configuration.setLocale(locale);
        
        //set font size
        if (TEXT_SIZE_LARGE.equals(savedTextSize)) {
            configuration.fontScale = 1.3f;
        } else {
            configuration.fontScale = 1.0f;
        }

        //apply config
        Context context = baseContext;

            context = baseContext.createConfigurationContext(configuration);

        
        super.attachBaseContext(context);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ensure settings are applied
        ((CampusApp) getApplicationContext()).applyAppSettings();

        // Find BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);

        // Find NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment)
                getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();

            // Set up BottomNavigationView with NavController
            NavigationUI.setupWithNavController(bottomNavigationView, navController);

            // If NavigationUI fails, manually handle tab selection
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                int id = item.getItemId();
                if (id == R.id.homeFragment) {
                    navController.navigate(R.id.homeFragment);
                    return true;
                } else if (id == R.id.exploreFragment) {
                    navController.navigate(R.id.exploreFragment);
                    return true;
                } else if (id == R.id.settingsFragment) {
                    navController.navigate(R.id.settingsFragment);
                    return true;
                } else if (id == R.id.mapFragment) {
                    navController.navigate(R.id.mapFragment);
                    return true;
                }
                return false;
            });
        } else {
            throw new IllegalStateException("NavHostFragment not found!");
        }
    }
}
