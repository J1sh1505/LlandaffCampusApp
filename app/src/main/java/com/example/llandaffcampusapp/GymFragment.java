package com.example.llandaffcampusapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;

public class GymFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflates the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_gym, container, false);

        // Find the "Learn More" buttons by their IDs
        MaterialButton learnMoreButtonGym1 = view.findViewById(R.id.learn_more_button_gym1);
        MaterialButton learnMoreButtonGym2 = view.findViewById(R.id.learn_more_button_gym2);

        // Set click listener for the first "Learn More" button
        learnMoreButtonGym1.setOnClickListener(v -> {
            // Open the URL for the first gym
            String url1 = "https://www.cardiffmet.ac.uk/met-sport/health-and-fitness/"; //
            openWebsite(url1);
        });

        // Set click listener for the second "Learn More" button
        learnMoreButtonGym2.setOnClickListener(v -> {
            // Open the URL for the second gym
            String url2 = "https://www.cardiffmet.ac.uk/met-sport/health-and-fitness/";
            openWebsite(url2);
        });

        return view;
    }

    // Helper method to open a URL in a web browser
    private void openWebsite(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(intent);
    }
}
