package com.example.llandaffcampusapp1;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.Locale;

public class SettingsFragment extends Fragment {
    private SharedPreferences preferences;
    private RadioGroup languageRadioGroup;
    private RadioGroup textSizeRadioGroup;

    private static final String PREF_NAME = "LlandaffCampusSettings";
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_TEXT_SIZE = "text_size";
    private static final String LANG_ENGLISH = "en";
    private static final String LANG_WELSH = "cy";
    private static final String TEXT_SIZE_NORMAL = "normal";
    private static final String TEXT_SIZE_LARGE = "large";

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

        //init views
        languageRadioGroup = view.findViewById(R.id.language_radio_group);
        textSizeRadioGroup = view.findViewById(R.id.text_size_radio_group);
        Button resetButton = view.findViewById(R.id.reset_button);
        TextView languageTitle = view.findViewById(R.id.language_title);
        TextView textResizingTitle = view.findViewById(R.id.text_resizing_title);

        //set load state based on preferences
        setupInitialState();

        //listeners
        languageRadioGroup.setOnCheckedChangeListener(this::onLanguageRadioButtonClicked);
        textSizeRadioGroup.setOnCheckedChangeListener(this::onTextSizeRadioButtonClicked);
        resetButton.setOnClickListener(this::resetSettings);

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
        
        //update app language
        setAppLanguage(languageCode);
        
        //restart activity to apply language change
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
            return; // No valid selection
        }

        preferences.edit().putString(PREF_TEXT_SIZE, textSize).apply();
        
        //apply text size change
        applyTextSize(textSize);
        
        // restart activity
        requireActivity().recreate();
    }

    private void resetSettings(View view) {
        //reset to default
        preferences.edit()
                .putString(PREF_LANGUAGE, LANG_ENGLISH)
                .putString(PREF_TEXT_SIZE, TEXT_SIZE_NORMAL)
                .apply();

        // same as above
        languageRadioGroup.check(R.id.english_radio);
        textSizeRadioGroup.check(R.id.normal_text_radio);
        
        setAppLanguage(LANG_ENGLISH);
        applyTextSize(TEXT_SIZE_NORMAL);
        
        requireActivity().recreate();
    }

    private void setAppLanguage(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);

        Resources resources = requireActivity().getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        
        //apply at app level, ensures settings persists
        if (getActivity() != null && getActivity().getApplication() instanceof CampusApp) {
            ((CampusApp) getActivity().getApplication()).applyAppSettings();
        }
    }

    private void applyTextSize(String textSize) {
        //get current config
        Resources resources = requireActivity().getResources();
        Configuration config = resources.getConfiguration();

        //apply text size
        if (TEXT_SIZE_LARGE.equals(textSize)) {
            config.fontScale = 1.3f; // Larger text
        } else {
            config.fontScale = 1.0f; // Normal text size
        }

        resources.updateConfiguration(config, resources.getDisplayMetrics());
        
        //apply at application level, ensures it persists
        if (getActivity() != null && getActivity().getApplication() instanceof CampusApp) {
            ((CampusApp) getActivity().getApplication()).applyAppSettings();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        //refresh when fragment appears
        setupInitialState();
    }
}