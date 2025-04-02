package com.example.llandaffcampusapp;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
