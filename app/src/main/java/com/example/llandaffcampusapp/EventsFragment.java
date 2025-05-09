package com.example.llandaffcampusapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class EventsFragment extends Fragment {

    private TextView registerMetHub1, registerMetHub2, registerMetHub3, registerMetHub4, registerMetHub5;
    private TextView exploreMoreMetHub;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflates the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_events, container, false);

        // Declare the TextViews
        registerMetHub1 = view.findViewById(R.id.register_methub_1);
        registerMetHub2 = view.findViewById(R.id.register_methub_2);
        registerMetHub3 = view.findViewById(R.id.register_methub_3);
        registerMetHub4 = view.findViewById(R.id.register_methub_4);
        registerMetHub5 = view.findViewById(R.id.register_methub_5);
        exploreMoreMetHub = view.findViewById(R.id.explore_more_methub);

        // Set up click listeners for each registration link with their respective URLs
        registerMetHub1.setOnClickListener(v -> openRegistrationPage("https://methub.cardiffmet.ac.uk/students/login?ReturnUrl=%2f"));
        registerMetHub2.setOnClickListener(v -> openRegistrationPage("https://methub.cardiffmet.ac.uk/students/login?ReturnUrl=%2f"));
        registerMetHub3.setOnClickListener(v -> openRegistrationPage("https://methub.cardiffmet.ac.uk/students/login?ReturnUrl=%2f"));
        registerMetHub4.setOnClickListener(v -> openRegistrationPage("https://methub.cardiffmet.ac.uk/students/login?ReturnUrl=%2f"));
        registerMetHub5.setOnClickListener(v -> openRegistrationPage("https://www.cardiffmet.ac.uk/about/sustainability/go-green-week/"));

        // click listener for "Explore more events"
        exploreMoreMetHub.setOnClickListener(v -> openRegistrationPage("https://methub.cardiffmet.ac.uk/students/login?ReturnUrl=%2f"));

        return view;
    }

    // Opens the provided URL in the users browser
    private void openRegistrationPage(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}
