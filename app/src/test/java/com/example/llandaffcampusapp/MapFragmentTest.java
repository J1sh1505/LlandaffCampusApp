package com.example.llandaffcampusapp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;


import static org.junit.Assert.*;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)

public class MapFragmentTest {

    private MapFragment mapFragment;

    @Before
    public void setUp() {
        mapFragment = new MapFragment();
    }

    @Test
    public void calculatePolygonCenter_emptyList_returnsNull() {
        List<GeoPoint> points = new ArrayList<>();
        GeoPoint center = mapFragment.calculatePolygonCenter(points);
        assertNull("Center of an empty polygon should be null", center);
    }

    @Test
    public void calculatePolygonCenter_singlePoint_returnsSamePoint() {
        List<GeoPoint> points = new ArrayList<>();
        GeoPoint singlePoint = new GeoPoint(10.0, 20.0);
        points.add(singlePoint);
        GeoPoint center = mapFragment.calculatePolygonCenter(points);
        assertNotNull("Center should not be null for a single point", center);
        assertEquals("Latitude should match", 10.0, center.getLatitude(), 0.001);
        assertEquals("Longitude should match", 20.0, center.getLongitude(), 0.001);
    }

    @Test
    public void calculatePolygonCenter_simpleSquare_returnsCorrectCenter() {
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(0.0, 0.0));
        points.add(new GeoPoint(0.0, 2.0));
        points.add(new GeoPoint(2.0, 2.0));
        points.add(new GeoPoint(2.0, 0.0));
        GeoPoint center = mapFragment.calculatePolygonCenter(points);
        assertNotNull("Center should not be null for a square", center);
        assertEquals("Center latitude should be 1.0", 1.0, center.getLatitude(), 0.001);
        assertEquals("Center longitude should be 1.0", 1.0, center.getLongitude(), 0.001);
    }

    @Test
    public void calculatePolygonCenter_anotherSquare_returnsCorrectCenter() {
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(51.4965, -3.2130));
        points.add(new GeoPoint(-51.4965, 3.2120));
        points.add(new GeoPoint(-51.4955, -3.2120));
        points.add(new GeoPoint(51.4955, 3.2130));
        GeoPoint center = mapFragment.calculatePolygonCenter(points);
        assertNotNull("Center should not be null for this square", center);
        assertEquals("Latitude for the second square's center is incorrect", 0.0, center.getLatitude(), 0.00001);
        assertEquals("Longitude for the second square's center is incorrect", 0.0, center.getLongitude(), 0.00001);
    }
}