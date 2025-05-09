package com.example.llandaffcampusapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeFragment extends Fragment {
    
    private static final String PREF_NAME = "LlandaffCampusSettings";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    private MaterialButton signInButton;

    public HomeFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        //set up sign in button
        signInButton = view.findViewById(R.id.sign_in_button2);
        updateSignInButtonVisibility();
        
        signInButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            startActivity(intent);
        });
        
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        updateSignInButtonVisibility();
    }
    
    private void updateSignInButtonVisibility() {
        if (getContext() == null) return;
        
        SharedPreferences preferences = getContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        boolean isLoggedIn = preferences.getBoolean(PREF_IS_LOGGED_IN, false);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        
        if (!isLoggedIn && currentUser == null) {
            signInButton.setVisibility(View.VISIBLE);
        } else {
            signInButton.setVisibility(View.GONE);
        }
    }
}
