package com.example.llandaffcampusapp1;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Overlay;

public class Searchable {
    public String name;         // e.g. "Room 101" or "Cafe"
    public String[] tags;       // e.g. ["room", "lecture", "101"]
    public String floor;        // e.g. "0" or "1" or "2"
    public Overlay overlay;     // The osmdroid overlay (Marker or Polygon)
    public GeoPoint center;     // For convenience, the center or bounding box

    // Constructor, getters, etc.
    public Searchable(String name, String[] tags, String floor, Overlay overlay, GeoPoint center) {
        this.name = name;
        this.tags = tags;
        this.floor = floor;
        this.overlay = overlay;
        this.center = center;
    }
}