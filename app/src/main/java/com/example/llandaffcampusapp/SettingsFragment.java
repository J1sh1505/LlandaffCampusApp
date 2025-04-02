package com.example.llandaffcampusapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsFragment extends Fragment {
    private static final String TAG = "SettingsFragment";
    private static final String USERS_COLLECTION = "users";
    private static final String SETTINGS_COLLECTION = "settings";
    
    private SharedPreferences preferences;
    private RadioGroup languageRadioGroup;
    private RadioGroup textSizeRadioGroup;
    private Button resetButton;
    private Button logoutButton;
    private TextView languageTitle;
    private TextView textResizingTitle;
    private TextView accountTitle;
    private TextView userEmailText;
    private CardView accountCard;

    private static final String PREF_NAME = "LlandaffCampusSettings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_TEXT_SIZE = "text_size";
    private static final String PREF_USER_ID = "user_id";
    private static final String PREF_IS_LOGGED_IN = "is_logged_in";
    private static final String LANG_ENGLISH = "en";
    private static final String LANG_WELSH = "cy";
    private static final String TEXT_SIZE_NORMAL = "normal";
    private static final String TEXT_SIZE_LARGE = "large";
    
    // Firebase components
    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestore;
    private GoogleSignInClient mGoogleSignInClient;

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        preferences = requireActivity().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        
        try {
            // Initialize Firebase components
            mAuth = FirebaseAuth.getInstance();
            mFirestore = FirebaseFirestore.getInstance();
            
            // Configure Google Sign-In
            GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail();
            
            try {
                // Try to use the default_web_client_id from strings.xml if it exists
                String webClientId = getString(R.string.default_web_client_id);
                if (webClientId != null && !webClientId.isEmpty()) {
                    gsoBuilder.requestIdToken(webClientId);
                    Log.d(TAG, "Using web client ID from strings.xml: " + webClientId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting default_web_client_id", e);
            }
            
            GoogleSignInOptions gso = gsoBuilder.build();
            mGoogleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Firebase", e);
        }

        //init views
        languageRadioGroup = view.findViewById(R.id.language_radio_group);
        textSizeRadioGroup = view.findViewById(R.id.text_size_radio_group);
        resetButton = view.findViewById(R.id.reset_button);
        logoutButton = view.findViewById(R.id.logout_button);
        languageTitle = view.findViewById(R.id.language_title);
        textResizingTitle = view.findViewById(R.id.text_resizing_title);
        accountTitle = view.findViewById(R.id.account_title);
        userEmailText = view.findViewById(R.id.user_email);
        accountCard = view.findViewById(R.id.account_card);



        //set load state based on preferences
        setupInitialState();
        setupAccountSection();

        //listeners
        languageRadioGroup.setOnCheckedChangeListener(this::onLanguageRadioButtonClicked);
        textSizeRadioGroup.setOnCheckedChangeListener(this::onTextSizeRadioButtonClicked);
        resetButton.setOnClickListener(this::resetSettings);
        logoutButton.setOnClickListener(this::logout);

        return view;
    }

    private void setupInitialState() {
        // language radio button
        String currentLanguage = preferences.getString(PREF_LANGUAGE, LANG_ENGLISH);
        if (LANG_ENGLISH.equals(currentLanguage)) {
            languageRadioGroup.check(R.id.english_radio);
        } else if (LANG_WELSH.equals(currentLanguage)) {
            languageRadioGroup.check(R.id.welsh_radio);
        }

        //text size radio button
        String currentTextSize = preferences.getString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL);
        if (TEXT_SIZE_NORMAL.equals(currentTextSize)) {
            textSizeRadioGroup.check(R.id.normal_text_radio);
        } else if (TEXT_SIZE_LARGE.equals(currentTextSize)) {
            textSizeRadioGroup.check(R.id.large_text_radio);
        }
    }
    
    private void setupAccountSection() {
        FirebaseUser currentUser = null;
        boolean isLoggedIn = preferences.getBoolean(PREF_IS_LOGGED_IN, false);

        try {
            if (mAuth != null) {
                currentUser = mAuth.getCurrentUser();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current user", e);
        }

        View view = getView(); //get pfp view
        if (view == null) return;

        CircleImageView profileImage = view.findViewById(R.id.profile_image);

        if (currentUser != null && isLoggedIn) {
            //user is logged in so display account details
            accountCard.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
            userEmailText.setText(currentUser.getEmail());

            Uri photoUrl = currentUser.getPhotoUrl();
            if (photoUrl != null && profileImage != null) {
                Glide.with(this)
                        .load(photoUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .error(R.drawable.ic_profile_placeholder)
                        .into(profileImage);
            }

        } else if (isLoggedIn) {
            //user logged in but Firebase user = null
            accountCard.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.VISIBLE);
            userEmailText.setText(R.string.guest_user);
        } else {
            // User is not logged in
            accountCard.setVisibility(View.GONE);
            logoutButton.setVisibility(View.GONE);
        }
    }

    private void onLanguageRadioButtonClicked(RadioGroup group, int checkedId) {
        //save lang preference
        String languageCode;
        if (checkedId == R.id.english_radio) {
            languageCode = LANG_ENGLISH;

        } else if (checkedId == R.id.welsh_radio) {
            languageCode = LANG_WELSH;
        } else {
            return; // no selection
        }

        preferences.edit().putString(PREF_LANGUAGE, languageCode).apply();
        syncSettingToFirebase(PREF_LANGUAGE, languageCode);
        
        //update app language
        setAppLanguage(languageCode);
        
        //restart activity, apply change
        requireActivity().recreate();
    }

    private void onTextSizeRadioButtonClicked(RadioGroup group, int checkedId) {
        //save text size preference
        String textSize;
        if (checkedId == R.id.normal_text_radio) {
            textSize = TEXT_SIZE_NORMAL;
        } else if (checkedId == R.id.large_text_radio) {
            textSize = TEXT_SIZE_LARGE;
        } else {
            return; //no selection
        }

        preferences.edit().putString(PREF_TEXT_SIZE, textSize).apply();
        
        //sync to Firebase if logged in
        syncSettingToFirebase(PREF_TEXT_SIZE, textSize);
        
        //apply text size change
        applyTextSize(textSize);
        
        //restart activity
        requireActivity().recreate();
    }

    private void resetSettings(View view) {
        //reset to default
        preferences.edit()
                .putString(PREF_LANGUAGE, LANG_ENGLISH)
                .putString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL)
                .apply();

        //sync change to firebase
        syncSettingToFirebase(PREF_LANGUAGE, LANG_ENGLISH);
        syncSettingToFirebase(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL);

        //same as above
        languageRadioGroup.check(R.id.english_radio);
        textSizeRadioGroup.check(R.id.normal_text_radio);
        
        setAppLanguage(LANG_ENGLISH);
        applyTextSize(TEXT_SIZE_NORMAL);
        
        requireActivity().recreate();
    }
    
    private void syncSettingToFirebase(String key, String value) {
        try {
            FirebaseUser currentUser = mAuth != null ? mAuth.getCurrentUser() : null;
            boolean isLoggedIn = preferences.getBoolean(PREF_IS_LOGGED_IN, false);
            
            if (currentUser != null && isLoggedIn && mFirestore != null) {
                String userId = currentUser.getUid();
                
                Map<String, Object> updates = new HashMap<>();
                updates.put(key, value);
                
                mFirestore.collection(USERS_COLLECTION)
                        .document(userId)
                        .collection(SETTINGS_COLLECTION)
                        .document("user_preferences")
                        .update(updates)
                        .addOnSuccessListener(aVoid -> Log.d(TAG, "Setting synced to Firestore"))
                        .addOnFailureListener(e -> {
                            mFirestore.collection(USERS_COLLECTION)
                                    .document(userId)
                                    .collection(SETTINGS_COLLECTION)
                                    .document("user_preferences")
                                    .set(updates)
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Created new settings document in Firestore"))
                                    .addOnFailureListener(e2 -> Log.w(TAG, "Error syncing settings", e2));
                        });
            }
        } catch (Exception e) {
            Log.e(TAG, "Error syncing to Firebase", e);
        }
    }

    private void logout(View view) {
        try {
            //sign out from Firebase
            if (mAuth != null) {
                mAuth.signOut();
            }
            
            //sign out from Google
            if (mGoogleSignInClient != null) {
                mGoogleSignInClient.signOut().addOnCompleteListener(task -> {
                    completeLogout();
                });
            } else {
                completeLogout();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error during logout", e);
            completeLogout();
        }
    }
    
    private void completeLogout() {
        //clear login state in preferences
        preferences.edit()
                .putBoolean(PREF_IS_LOGGED_IN, false)
                .remove(PREF_USER_ID)
                .apply();
        
        //redirect to login screen
        Intent intent = new Intent(requireActivity(), LoginActivity.class);
        startActivity(intent);
        requireActivity().finish();
    }

    private void setAppLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = requireActivity().getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    private void applyTextSize(String textSize) {
        //get current config
        Resources resources = requireActivity().getResources();
        Configuration config = resources.getConfiguration();

        //apply text size
        if (TEXT_SIZE_LARGE.equals(textSize)) {
            config.fontScale = 1.3f; //LARGER text
        } else {
            config.fontScale = 1.0f; //normal text size
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    @Override
    public void onResume() {
        super.onResume();
        //refresh when fragment appears
        setupInitialState();
        setupAccountSection();
    }
}