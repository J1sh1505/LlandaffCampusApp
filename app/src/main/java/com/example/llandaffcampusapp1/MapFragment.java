package com.example.llandaffcampusapp1;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {
    private MapView mapView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(18.0);
        GeoPoint startPoint = new GeoPoint(51.496206, -3.213042); // llandaff campus
        mapController.setCenter(startPoint);

        // Set up the buttons for floor selection -- change later
        Button btnGroundFloor = view.findViewById(R.id.btnGroundFloor);
        Button btnFirstFloor = view.findViewById(R.id.btnFirstFloor);
        Button btnSecondFloor = view.findViewById(R.id.btnSecondFloor);

        btnGroundFloor.setOnClickListener(v -> loadFloorData("0"));
        btnFirstFloor.setOnClickListener(v -> loadFloorData("1"));
        btnSecondFloor.setOnClickListener(v -> loadFloorData("2"));

        // Loads ground floor by default
        loadFloorData("0");

        // Load the custom GeoJSON data and add its features to the map.
        JSONObject geoJson = GeoJsonUtils.loadGeoJsonFromAsset(getContext(), "campus_floor_0.geojson");
        addGeoJsonFeatures(geoJson);

        return view;
    }
    private void loadFloorData(String floorNumber) {
        // Clear current overlays
        mapView.getOverlays().clear();

        // Decide which file to load
        String filename = "campus_floor_" + floorNumber + ".geojson";

        // Load the file from assets
        JSONObject geoJson = GeoJsonUtils.loadGeoJsonFromAsset(getContext(), filename);

        // Parse and add its features
        addGeoJsonFeatures(geoJson);

        // Refresh the map
        mapView.invalidate();
    }


    /**
     * Parses GeoJSON features and adds them (markers, polygons, etc.) to the map.
     */
    private void addGeoJsonFeatures(JSONObject geoJson) {
        if (geoJson == null) return;
        try {
            JSONArray features = geoJson.getJSONArray("features");

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONObject properties = feature.optJSONObject("properties");
                String type = geometry.getString("type");

                switch (type) {
                    case "Point":
                        drawPoint(geometry.getJSONArray("coordinates"), properties);
                        break;

                    case "Polygon":
                        drawPolygon(geometry.getJSONArray("coordinates"), properties);
                        break;


                    // Optionally handle other geometry types (e.g., LineString)
                    default:
                        Log.w("MapFragment", "Unsupported geometry type: " + type);
                        break;
                }
            }

            mapView.invalidate(); // Refresh the map
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int getIconResource(String iconType) {
        switch (iconType) {
            case "my_custom_icon":
            case "inf_desk":
                return R.drawable.ic_inf_desk;
            case "library":

            // Add more as needed
            default:
                return 0; // 0 means not found
        }
    }
    private void drawPoint(JSONArray coords, JSONObject properties) {
        try {
            double lon = coords.getDouble(0);
            double lat = coords.getDouble(1);
            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(lat, lon));

            // Read the "icon" or "icon:url" property from GeoJSON
            String iconType = properties != null ? properties.optString("icon", null) : null;
            if (iconType != null) {
                int iconResId = getIconResource(iconType);
                if (iconResId != 0) {
                    marker.setIcon(ContextCompat.getDrawable(requireContext(), iconResId));
                }
            }

            // Title, snippet, etc.
            marker.setTitle(properties.optString("name", "No Name"));
            mapView.getOverlays().add(marker);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void drawPolygon(JSONArray coordinates, JSONObject properties) {
        try {
            // coordinates[0] is typically the outer ring. Additional rings are holes.
            JSONArray outerRing = coordinates.getJSONArray(0);
            Polygon polygon = new Polygon(mapView);

            List<GeoPoint> geoPoints = new ArrayList<>();
            for (int i = 0; i < outerRing.length(); i++) {
                JSONArray point = outerRing.getJSONArray(i);
                double lon = point.getDouble(0);
                double lat = point.getDouble(1);
                geoPoints.add(new GeoPoint(lat, lon));
            }
            polygon.setPoints(geoPoints);



            // Basic property-based styling
            int fillColor = getFillColor(properties);
            int strokeColor = getStrokeColor(properties);
            float strokeWidth = getStrokeWidth(properties);

            polygon.setFillColor(fillColor);
            polygon.setStrokeColor(strokeColor);
            polygon.setStrokeWidth(strokeWidth);

            // Optional: if you'd like to show a label when tapped
            String name = properties != null ? properties.optString("name", "No Name") : "No Name";
            polygon.setTitle(name);
            polygon.setOnClickListener((Polygon p, MapView mapView, GeoPoint eventPos) -> {
                // Show the default InfoWindow
                p.showInfoWindow();
                return true;
            });

            mapView.getOverlays().add(polygon);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    // ========== Helper Methods for Customization ========== //

    /**
     * Returns a fill color based on the properties, defaulting to a semi-transparent green.
     */
    private int getFillColor(JSONObject properties) {
        if (properties == null) return 0x3300FF00; // 20% opacity green
        if (properties.has("fillColor")) {
            String colorStr = properties.optString("fillColor", "#33FF00"); // hex code
            return parseColor(colorStr, 0x3300FF00);
        }
        // Other property-based logic can go here (e.g., category-based colors)
        return 0x3300FF00;
    }

    /**
     * Returns a stroke color based on the properties, defaulting to solid green.
     */
    private int getStrokeColor(JSONObject properties) {
        if (properties == null) return 0xFF00FF00;
        if (properties.has("strokeColor")) {
            String colorStr = properties.optString("strokeColor", "#00FF00");
            return parseColor(colorStr, 0xFF00FF00);
        }
        return 0xFF00FF00;
    }

    /**
     * Returns a stroke width based on the properties, defaulting to 2.0f.
     */
    private float getStrokeWidth(JSONObject properties) {
        if (properties == null) return 2.0f;
        if (properties.has("strokeWidth")) {
            return (float) properties.optDouble("strokeWidth", 2.0);
        }
        return 2.0f;
    }

    /**
     * Parses a hex color string (e.g., "#RRGGBB" or "#AARRGGBB") into an int.
     */
    private int parseColor(String colorStr, int defaultColor) {
        try {
            return Color.parseColor(colorStr);
        } catch (Exception e) {
            e.printStackTrace();
            return defaultColor;
        }
    }



    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}
