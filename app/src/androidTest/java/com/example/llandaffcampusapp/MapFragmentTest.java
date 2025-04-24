package com.example.llandaffcampusapp;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;

import java.util.Objects;

@RunWith(AndroidJUnit4.class)
public class MapFragmentTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Test map loads with the right settings initially:
     * - Map centered over campus
     * - Zoom is correct
     * - Map is stuck in it's box/boundaries
     */
    @Test
    public void testMapInitialSettings() {
        // nav to map
        onView(withId(R.id.mapFragment)).perform(click());

        // Get activity and fragment
        activityRule.getScenario().onActivity(activity -> {
            // Find map fragment
            MapFragment mapFragment = (MapFragment) activity.getSupportFragmentManager()
                    .findFragmentById(R.id.nav_host_fragment)
                    .getChildFragmentManager()
                    .getFragments().get(0);

            // Get map 
            MapView mapView = mapFragment.getView().findViewById(R.id.mapView);

            // Check map centered over campus
            GeoPoint center = (GeoPoint) mapView.getMapCenter();
            double lat = center.getLatitude();
            double lon = center.getLongitude();

            // Check within range
            assertTrue("Latitude should be over Llandaff campus", lat >= 51.493856 && lat <= 51.498556);
            assertTrue("Longitude should be over Llandaff campus", lon >= -3.216342 && lon <= -3.209742);

            // Check zoom level is Ok
            double zoom = mapView.getZoomLevelDouble();
            assertTrue("Zoom level should be in limits", zoom >= 18.1 && zoom <= 22.0);

            // Check floor is set to ground floor
            assertEquals("Floor should be ground floor", "0", mapFragment.getCurrentFloor());
        });
    }

    /**
     * Test floor switching 
     */
    @Test
    public void testMapFloorSwitching() {
        // nav to map fragment
        onView(withId(R.id.mapFragment)).perform(click());

        // Check floor switching
        // Click first floor
        onView(withId(R.id.btnFirstFloor)).perform(click());

        // Check floor changed
        activityRule.getScenario().onActivity(activity -> {
            MapFragment mapFragment = (MapFragment) Objects.requireNonNull(activity.getSupportFragmentManager()
                            .findFragmentById(R.id.nav_host_fragment))
                    .getChildFragmentManager()
                    .getFragments().get(0);

            assertEquals("Floor should've changed to 1", "1", mapFragment.getCurrentFloor());
        });

        // Click second floor button
        onView(withId(R.id.btnSecondFloor)).perform(click());

        // check floor changed
        activityRule.getScenario().onActivity(activity -> {
            MapFragment mapFragment = (MapFragment) Objects.requireNonNull(activity.getSupportFragmentManager()
                            .findFragmentById(R.id.nav_host_fragment))
                    .getChildFragmentManager()
                    .getFragments().get(0);

            assertEquals("Floor should've changed to 2", "2", mapFragment.getCurrentFloor());
        });

        // Go to ground floor
        onView(withId(R.id.btnGroundFloor)).perform(click());

        // Check floor was changed
        activityRule.getScenario().onActivity(activity -> {
            MapFragment mapFragment = (MapFragment) Objects.requireNonNull(activity.getSupportFragmentManager()
                            .findFragmentById(R.id.nav_host_fragment))
                    .getChildFragmentManager()
                    .getFragments().get(0);

            assertEquals("Floor should be changed to 0", "0", mapFragment.getCurrentFloor());
        });
    }
}
