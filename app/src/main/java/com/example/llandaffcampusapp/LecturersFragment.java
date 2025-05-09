package com.example.llandaffcampusapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

public class LecturersFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_lecturers, container, false);

        // Animation for cards
        LinearLayout cardContainer = view.findViewById(R.id.card_container);
        for (int i = 0; i < cardContainer.getChildCount(); i++) {
            View card = cardContainer.getChildAt(i);
            if (card instanceof CardView) {
                card.setAlpha(0f);
                card.setTranslationY(100);
                card.animate()
                        .alpha(1f)
                        .translationY(0)
                        .setStartDelay(i * 100)
                        .setDuration(400)
                        .setInterpolator(new AccelerateDecelerateInterpolator())
                        .start();
            }
        }

        // Lecturer profile links
        TextView viewProfileJohn = view.findViewById(R.id.view_profile_john_doe);//Faizan Ahmad
        TextView viewProfileJane = view.findViewById(R.id.view_profile_jane_smith);//Paul Jenkins
        TextView viewProfileAlan = view.findViewById(R.id.view_profile_alan_brown);//Emma Bettinson

        viewProfileJohn.setOnClickListener(v -> openUrl("https://www.cardiffmet.ac.uk/staff/faizan-ahmad/"));
        viewProfileJane.setOnClickListener(v -> openUrl("https://www.cardiffmet.ac.uk/staff/paul-jenkins/"));
        viewProfileAlan.setOnClickListener(v -> openUrl("https://www.cardiffmet.ac.uk/staff/emma-bettinson/"));

        // Platform link
        TextView visitPlatformLink = view.findViewById(R.id.visit_platform_link);
        visitPlatformLink.setOnClickListener(v -> openUrl("https://www.cardiffmet.ac.uk/staff/"));

        return view;
    }

    private void openUrl(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }
}
