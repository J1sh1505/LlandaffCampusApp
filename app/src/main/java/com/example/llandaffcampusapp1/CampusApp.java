package com.example.llandaffcampusapp1;
import android.app.Application;

import org.osmdroid.config.Configuration;

public class CampusApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Set the user agent to your app's package name or another unique identifier.
        Configuration.getInstance().setUserAgentValue(getPackageName());
    }
}
