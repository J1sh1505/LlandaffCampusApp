package com.example.llandaffcampusapp1;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {
    private MapView mapView;
    private Runnable zoomUpdateRunnable;
    private final Handler handler = new Handler(Looper.getMainLooper());

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
        //TODO: Change to MapEventsReceiver as this is deprecated
        mapView.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                //cancel any pending updates
                if (zoomUpdateRunnable != null) {
                    handler.removeCallbacks(zoomUpdateRunnable);
                }
                // Schedule a single update after zooming stops
                //instead of updating every time the zoom level changes
                //which could be multiple times a second with pinch to zoom
                //leading to dropped frames/performance issues
                zoomUpdateRunnable = () -> loadFloorData(getCurrentFloor());
                handler.postDelayed(zoomUpdateRunnable, 300);
                return false;
            }

        });

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
        setCurrentFloor(floorNumber);

        // Clear overlays and views
        mapView.getOverlays().clear();

        // Remove all TextView labels
        for (int i = mapView.getChildCount() - 1; i >= 0; i--) {
            View child = mapView.getChildAt(i);
            if (child instanceof TextView) {
                mapView.removeView(child);
            }
        }

        String filename = "campus_floor_" + floorNumber + ".geojson";
        JSONObject geoJson = GeoJsonUtils.loadGeoJsonFromAsset(getContext(), filename);
        addGeoJsonFeatures(geoJson);
        mapView.invalidate();
    }

    private void addBuildingLabel(GeoPoint position, String buildingLetter) {
        TextView label = new TextView(requireContext());
        label.setText(buildingLetter);


        double currentZoom = mapView.getZoomLevelDouble();
        float textSize = (float)(currentZoom * 1.2); //text size
        label.setTextSize(textSize);

        //styling: background, text colour of block labels
        label.setTextColor(Color.BLACK);
        label.setBackgroundColor(Color.TRANSPARENT);
        label.setPadding(8, 4, 8, 4);
        label.setTypeface(null, Typeface.BOLD);

        MapView.LayoutParams params = new MapView.LayoutParams(
                MapView.LayoutParams.WRAP_CONTENT,
                MapView.LayoutParams.WRAP_CONTENT,
                position,
                MapView.LayoutParams.CENTER,
                0, 0);

        mapView.addView(label, params);
    }

    /**
     * Parses GeoJSON features and adds them (markers, polygons, etc.) to the map.
     */
    private void addGeoJsonFeatures(JSONObject geoJson) {
        if (geoJson == null) return;
        try {
            JSONArray features = geoJson.getJSONArray("features");
            double currentZoom = mapView.getZoomLevelDouble();
            boolean showDetails = currentZoom >= 20.0;  // Only show rooms at zoom level 20 or more

            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONObject properties = feature.optJSONObject("properties");
                String type = geometry.getString("type");

                // Check for building label nodes
                //Node must have type "Point" and have a"type" property with value of "building"
                //and a "building_letter" property with the correct block
                if ("Point".equals(type) &&
                        properties != null &&
                        "building".equals(properties.optString("type"))) {

                    String buildingLetter = properties.optString("building_letter", "");
                    if (!buildingLetter.isEmpty()) {
                        JSONArray coords = geometry.getJSONArray("coordinates");
                        double lon = coords.getDouble(0);
                        double lat = coords.getDouble(1);
                        addBuildingLabel(new GeoPoint(lat, lon), buildingLetter);
                    }
                    continue;  // Skip further processing for building label points
                }

                // Handle other feature types
                switch (type) {
                    case "Point":
                        // Only draw points (labels) when zoomed in
                        if (showDetails) {
                            drawPoint(geometry.getJSONArray("coordinates"), properties);
                        }
                        break;

                    case "LineString":
                        // For room dividers/walls
                        if (showDetails) {
                            drawLine(geometry.getJSONArray("coordinates"), properties);
                        }
                        break;

                    case "Polygon":
                        drawPolygon(geometry.getJSONArray("coordinates"), properties);
                        break;
                }
            }
            mapView.invalidate();
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

            // Skip marker creation, only create label if name exists
            if (properties != null && properties.has("name")) {
                TextView label = new TextView(requireContext());
                label.setText(properties.getString("name"));
                label.setTextSize(16);
                label.setTextColor(Color.BLACK);

                // Create a layout parameter that positions the text at the coordinates
                MapView.LayoutParams params = new MapView.LayoutParams(
                        MapView.LayoutParams.WRAP_CONTENT,
                        MapView.LayoutParams.WRAP_CONTENT,
                        new GeoPoint(lat, lon),
                        MapView.LayoutParams.CENTER,
                        0, 0);

                mapView.addView(label, params);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void drawPolygon(JSONArray coordinates, JSONObject properties) {
        try {
            double currentZoom = mapView.getZoomLevelDouble();
            boolean detailed = currentZoom >= 18.0;

            JSONArray outerRing = coordinates.getJSONArray(0);
            Polygon polygon = new Polygon(mapView);  // Define the polygon object

            List<GeoPoint> geoPoints = new ArrayList<>();
            for (int i = 0; i < outerRing.length(); i++) {
                JSONArray point = outerRing.getJSONArray(i);
                double lon = point.getDouble(0);
                double lat = point.getDouble(1);
                geoPoints.add(new GeoPoint(lat, lon));
            }
            polygon.setPoints(geoPoints);

            Paint outlinePaint = polygon.getOutlinePaint();
            outlinePaint.setColor(getStrokeColor(properties));
            outlinePaint.setStrokeWidth(detailed ? 5f : 2f);

            Paint fillPaint = polygon.getFillPaint();
            fillPaint.setColor(detailed ? getFillColor(properties) : Color.TRANSPARENT);

            mapView.getOverlays().add(polygon);

            polygon.setOnClickListener(new Polygon.OnClickListener() {
                @Override
                public boolean onClick(Polygon polygon, MapView mapView, GeoPoint eventPos) {
                    // Consume tap events to prevent annoying speech bubble from
                    // appearing when tapping areas overlaid with polygons
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void drawLine(JSONArray coordinates, JSONObject properties) {
        try {
            Polyline line = new Polyline(mapView);
            List<GeoPoint> points = new ArrayList<>();

            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray point = coordinates.getJSONArray(i);
                double lon = point.getDouble(0);
                double lat = point.getDouble(1);
                points.add(new GeoPoint(lat, lon));
            }

            line.setPoints(points);
            // Set the outline color and width
            // changed to Paint
            Paint paint = line.getOutlinePaint();
            paint.setColor(Color.rgb(0, 255, 0));
            paint.setStrokeWidth(8f);

            mapView.getOverlays().add(line);

            line.setOnClickListener(new Polyline.OnClickListener() {
                @Override
                public boolean onClick(Polyline polyline, MapView mapView, GeoPoint eventPos) {
                    // Same as for polygons, consume tap events to prevent annoying speech bubbles
                    //from appearing when lines are tapped
                    return true;
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // ========== Helper Methods for Customization ========== //

    /**
     * Returns a fill color based on the properties, defaulting to a semi-transparent green.
     */
    private int getFillColor(JSONObject properties) {
        if (properties == null) return Color.TRANSPARENT; // No fill at all

        if (properties.has("fillColor")) {
            String colorStr = properties.optString("fillColor", "#00000000");
            return parseColor(colorStr, Color.TRANSPARENT);
        }

        // Check if this is a building or room
        String type = properties.optString("type", "");
        if (type.equals("building") || type.equals("room")) {
            return Color.argb(32, 0, 0, 0); // Very transparent black
        }

        // Default to transparent for all other polygons
        return Color.TRANSPARENT;
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
    private String currentFloor = "0";  // Default to ground floor

    private String getCurrentFloor() {
        return currentFloor;
    }

    private void setCurrentFloor(String floor) {
        currentFloor = floor;
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
