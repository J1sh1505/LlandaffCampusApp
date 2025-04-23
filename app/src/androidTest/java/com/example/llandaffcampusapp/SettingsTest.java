package com.example.llandaffcampusapp;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.espresso.intent.Intents;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.SystemClock;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests settings are saved and applied
 */
@RunWith(AndroidJUnit4.class)
public class SettingsTest {
    
    private static final String PREF_NAME = "LlandaffCampusSettings";
    private static final String PREF_TEXT_SIZE = "text_size";
    private static final String PREF_LANGUAGE = "language";
    
    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = 
            new ActivityScenarioRule<>(MainActivity.class);
    
    private SharedPreferences preferences;
    
    @Before
    public void setup() {
        Intents.init();
    }
    
    @After
    public void cleanup() {
        Intents.release();
    }
    
    /**
     * Test text size is applied and saved
     */
    @Test
    public void testTextSizeSettings() {
        // nav to settings fragment
        onView(withId(R.id.settingsFragment)).perform(click());
        
        // Get init font scale
        final float[] initialFontScale = new float[1];
        activityRule.getScenario().onActivity(activity -> {
            initialFontScale[0] = activity.getResources().getConfiguration().fontScale;
            preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        });
        
        // Click "Large Text" 
        onView(withId(R.id.large_text_radio)).perform(click());
        
        // Wait
        SystemClock.sleep(2000);
        
        // check font size changed
        activityRule.getScenario().onActivity(activity -> {
            float newFontScale = activity.getResources().getConfiguration().fontScale;
            assertTrue("Font size should be higher", newFontScale > initialFontScale[0]);
            
            // check setting saved in preferences
            preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String savedTextSize = preferences.getString(PREF_TEXT_SIZE, "");
            assertEquals("Text size should be saved as large", "large", savedTextSize);
        });
        
        // Click Normal Text 
        onView(withId(R.id.normal_text_radio)).perform(click());
        
        // Wait
        SystemClock.sleep(2000);
        
        // Check font scale reset
        activityRule.getScenario().onActivity(activity -> {
            float newFontScale = activity.getResources().getConfiguration().fontScale;
            //normal size
            assertTrue("Font size should be normal", Math.abs(newFontScale - 1.0f) < 0.1f);
            
            // check saved to preferences
            String savedTextSize = preferences.getString(PREF_TEXT_SIZE, "");
            assertEquals("Text size  should be normal", "normal", savedTextSize);
        });
    }
    
    /**
     * Test language settings are applied and saved
     */
    @Test
    public void testLanguageSettings() {
        // Nav to the settings fragment
        onView(withId(R.id.settingsFragment)).perform(click());
        
        // Get init lang
        final String[] initialLanguage = new String[1];
        activityRule.getScenario().onActivity(activity -> {
            Configuration config = activity.getResources().getConfiguration();
            initialLanguage[0] = config.getLocales().get(0).getLanguage();
            preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        });
        
        // Click Welsh
        onView(withId(R.id.welsh_radio)).perform(click());
        
        // Wait
        SystemClock.sleep(2000);
        
        // check lang changed
        activityRule.getScenario().onActivity(activity -> {
            Configuration config = activity.getResources().getConfiguration();
            String newLanguage = config.getLocales().get(0).getLanguage();
            
            // check setting was saved
            preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String savedLanguage = preferences.getString(PREF_LANGUAGE, "");
            assertEquals("Language preference should be saved as Welsh", newLanguage, savedLanguage);
        });
        
        // Click English
        onView(withId(R.id.english_radio)).perform(click());
        
        // Wait
        SystemClock.sleep(2000);

        //check saved to prefs
        activityRule.getScenario().onActivity(activity -> {
            preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String savedLanguage = preferences.getString(PREF_LANGUAGE, "");
            assertEquals("Language preference should be saved as English", "en", savedLanguage);
        });
    }
    
    /**
     * Test to test reset
     */
    @Test
    public void testSettingsReset() {
        // Nav to settings
        onView(withId(R.id.settingsFragment)).perform(click());
        
        // change settings to be not default
        onView(withId(R.id.large_text_radio)).perform(click());
        SystemClock.sleep(1000);
        onView(withId(R.id.welsh_radio)).perform(click());
        SystemClock.sleep(2000);
        
        // check settings changed
        activityRule.getScenario().onActivity(activity -> {
            preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String textSize = preferences.getString(PREF_TEXT_SIZE, "");
            String language = preferences.getString(PREF_LANGUAGE, "");
            
            assertEquals("Text size should be large", "large", textSize);
            assertEquals("Language should be Welsh", "cy", language);
        });
        
        // scroll to and lick reset button
        onView(withId(R.id.reset_button))
                .perform(scrollTo(), click());
        
        // Wait for settings to apply
        SystemClock.sleep(2000);
        
        // check if settings reset to defaults
        activityRule.getScenario().onActivity(activity -> {
            preferences = activity.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String textSize = preferences.getString(PREF_TEXT_SIZE, "");
            String language = preferences.getString(PREF_LANGUAGE, "");
            
            assertEquals("Text size should be normal", "normal", textSize);
            assertEquals("Language should be English", "en", language);
            
            // Check UI state is also reset
            float fontScale = activity.getResources().getConfiguration().fontScale;
            assertTrue("Font size should be normal", Math.abs(fontScale - 1.0f) < 0.1f);
            
            Configuration config = activity.getResources().getConfiguration();
            String currentLanguage = config.getLocales().get(0).getLanguage();
            assertEquals("lang should be english", "en", currentLanguage);
        });
    }
}
