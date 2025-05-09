package com.example.llandaffcampusapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.llandaffcampusapp.R;

public class ExploreFragment extends Fragment {

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Navigate to Lecture Halls
        view.findViewById(R.id.lecture_halls_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_exploreFragment_to_lectureHallsFragment)
        );

        // Navigate to Cafeteria
        view.findViewById(R.id.cafeterias_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_exploreFragment_to_cafeteriaFragment)
        );

        // Navigate to Lectures
        view.findViewById(R.id.lecturers_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_exploreFragment_to_lecturersFragment)
        );

        // Navigate to Gym
        view.findViewById(R.id.gym_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_exploreFragment_to_gymFragment)
        );

        // Navigate to Technology
        view.findViewById(R.id.technology_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_exploreFragment_to_technologyFragment)
        );
        // Navigate to Location Blocks
        view.findViewById(R.id.location_blocks_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_exploreFragment_to_locationBlocksFragment)
        );

        // Navigate to Events
        view.findViewById(R.id.events_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_exploreFragment_to_eventsFragment)
        );

        // Navigate to Meetings
        view.findViewById(R.id.meetings_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_exploreFragment_to_meetingsFragment)
        );

        // Navigate to Book an Induction
        view.findViewById(R.id.book_induction_button).setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_exploreFragment_to_bookInductionFragment)
        );
    }

    public ExploreFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_explore, container, false);
    }
}
