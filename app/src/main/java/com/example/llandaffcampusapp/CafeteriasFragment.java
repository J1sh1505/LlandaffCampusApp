package com.example.llandaffcampusapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;

public class CafeteriasFragment extends Fragment {

    // Declare all buttons
    private MaterialButton learnMoreButton1, learnMoreButton2, learnMoreButton3, bottomButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflates the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_cafeterias, container, false);

        // Initialize buttons
        learnMoreButton1 = view.findViewById(R.id.learn_more_button_1);
        learnMoreButton2 = view.findViewById(R.id.learn_more_button_2);
        learnMoreButton3 = view.findViewById(R.id.learn_more_button_3);
        bottomButton = view.findViewById(R.id.bottom_button);

        // Set up click listeners for each "Learn More" button
        learnMoreButton1.setOnClickListener(v -> openCafeteriaLink("https://www.cardiffmet.ac.uk/business/conference-services/hospitality/"));
        learnMoreButton2.setOnClickListener(v -> openCafeteriaLink("https://www.cardiffmet.ac.uk/business/conference-services/hospitality/"));
        learnMoreButton3.setOnClickListener(v -> openCafeteriaLink("https://www.cardiffmet.ac.uk/business/conference-services/hospitality/"));

        // click listener for the "Find out more" button
        bottomButton.setOnClickListener(v -> openCafeteriaLink("https://www.cardiffmet.ac.uk/business/conference-services/hospitality/"));

        return view;
    }

    // Method to open the URL on browser
    private void openCafeteriaLink(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}
