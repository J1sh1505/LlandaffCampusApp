package com.example.llandaffcampusapp;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;

public class MapFragment extends Fragment {
    private MapView mapView;
    private Runnable zoomUpdateRunnable;
    private final Handler handler = new Handler(Looper.getMainLooper());


    // coordinates form a box around the campus
    //confining users to just the llandaff campus
    private static final double NORTH_LAT = 51.498556; // north
    private static final double SOUTH_LAT = 51.493856; // south
    private static final double WEST_LON = -3.216342;  // west
    private static final double EAST_LON = -3.209742;  // east

    // Define zoom level limits
    private static final double MIN_ZOOM = 18.1; //min zoom
    private static final double MAX_ZOOM = 22.0; // max zoom (closest in)

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_map, container, false);
        mapView = view.findViewById(R.id.mapView);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        //set zoom limits
        mapView.setMinZoomLevel(MIN_ZOOM);
        mapView.setMaxZoomLevel(MAX_ZOOM);

        //create boundary box
        final BoundingBox campusBounds = new BoundingBox(
                NORTH_LAT, EAST_LON, SOUTH_LAT, WEST_LON);
        mapView.setScrollableAreaLimitDouble(campusBounds);

        IMapController mapController = mapView.getController();
        mapController.setZoom(18.1);
        GeoPoint startPoint = new GeoPoint(51.496206, -3.213042); // llandaff campus
        mapController.setCenter(startPoint);

        //TODO: Change to MapEventsReceiver as this is deprecated
        mapView.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                // enforce bounds always
                enforceMapBounds();
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
                zoomUpdateRunnable = () -> {
                    enforceZoomLimits();
                    loadFloorData(getCurrentFloor());
                };
                handler.postDelayed(zoomUpdateRunnable, 50);
                return false;
            }
        });

        // Set up the buttons for floor selection -- change later
        Button btnGroundFloor = view.findViewById(R.id.btnGroundFloor);
        Button btnFirstFloor = view.findViewById(R.id.btnFirstFloor);
        Button btnSecondFloor = view.findViewById(R.id.btnSecondFloor);
        Button btnLegend = view.findViewById(R.id.btnLegend);

        btnGroundFloor.setOnClickListener(v -> loadFloorData("0"));
        btnFirstFloor.setOnClickListener(v -> loadFloorData("1"));
        btnSecondFloor.setOnClickListener(v -> loadFloorData("2"));
        
        //init Legend btn, nav to Legend/index
        btnLegend.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(view)
                .navigate(R.id.action_mapFragment_to_mapIndexFragment);
        });

        // Loads ground floor by default
        loadFloorData("0");

        // Load the custom GeoJSON data and add its features to the map.
        JSONObject geoJson = GeoJsonUtils.loadGeoJsonFromAsset(getContext(), "campus_floor_0.geojson");
        addGeoJsonFeatures(geoJson);

        return view;
    }

    /**
     * backup/verify if map bugs out and crosses out of bounds
     */
    private void enforceMapBounds() {
        if (mapView == null) return;

        // get  center
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        double lat = center.getLatitude();
        double lon = center.getLongitude();

        // check if centre needs adjusting
        boolean needsAdjustment = false;

        // clamp latitude
        if (lat > NORTH_LAT) {
            lat = NORTH_LAT;
            needsAdjustment = true;
        } else if (lat < SOUTH_LAT) {
            lat = SOUTH_LAT;
            needsAdjustment = true;
        }

        // clamp longitude
        if (lon < WEST_LON) {
            lon = WEST_LON;
            needsAdjustment = true;
        } else if (lon > EAST_LON) {
            lon = EAST_LON;
            needsAdjustment = true;
        }

        // if OOB, animate to centre
        if (needsAdjustment) {
            final GeoPoint adjustedCenter = new GeoPoint(lat, lon);
            mapView.getController().animateTo(adjustedCenter);
        }
    }

    /**
     * backup/verify zoom level doesn't bug out/go OOB
     */
    private void enforceZoomLimits() {
        if (mapView == null) return;

        double currentZoom = mapView.getZoomLevelDouble();

        if (currentZoom < MIN_ZOOM) {
            mapView.getController().setZoom(MIN_ZOOM);
        } else if (currentZoom > MAX_ZOOM) {
            mapView.getController().setZoom(MAX_ZOOM);
        }
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
            boolean showDetails = currentZoom >= 20.5;  // Only show rooms at zoom level 20.5 or more

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
                        // Only draw points (labels or icons) when zoomed in
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
        if (iconType == null || iconType.isEmpty()) {
            return 0;
        }

        switch (iconType) {
            case "information":
            case "info":
                return R.drawable.ic_inf_desk;
            case "library":
                return R.drawable.ic_library2;
            case "stairs":
                return R.drawable.ic_stairs;
            case "food":
                return R.drawable.ic_food;
            case "computer":
                return R.drawable.ic_computer;
            case "toilet":
            case "restroom":
                return R.drawable.ic_toilet;
            case "accessible":
                return R.drawable.ic_accessible;
            case "coffee":
                return R.drawable.ic_coffee;
            case "table":
                return R.drawable.ic_tables;
            case "elevator":
                return R.drawable.ic_elevator;
            case "gym":
                return R.drawable.ic_gym;
            case "it":
                return R.drawable.ic_it;
            case "parking":
                return R.drawable.ic_parking;
            case "lecture":
                return R.drawable.ic_lecture;
            default:
                return 0; //not found
        }
    }

    private void drawPoint(JSONArray coords, JSONObject properties) {
        try {
            double lon = coords.getDouble(0);
            double lat = coords.getDouble(1);
            GeoPoint point = new GeoPoint(lat, lon);

            //check if icon specified in properties
            String iconType = properties != null ? properties.optString("icon_type", "") : "";
            int iconResource = getIconResource(iconType);

            if (iconResource != 0) {
                //create marker with icon
                Marker marker = new Marker(mapView);
                marker.setPosition(point);

                //set icon
                Drawable icon = ContextCompat.getDrawable(requireContext(), iconResource);
                marker.setIcon(icon);

                //set anchor to center-bottom of icon
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                //set title if name is available (for popup info)
                if (properties != null && properties.has("name")) {
                    marker.setTitle(properties.getString("name"));
                }
                //add to map
                mapView.getOverlays().add(marker);


            } else if (properties != null && properties.has("name")) {
                //if no icon but has name crate a text label
                TextView label = new TextView(requireContext());
                label.setText(properties.getString("name"));
                label.setTextSize(10);
                label.setTextColor(Color.BLACK);
                label.setBackgroundColor(Color.TRANSPARENT);
                label.setPadding(8, 4, 8, 4);

                // make a layout parameter - positions text at coordinates
                MapView.LayoutParams params = new MapView.LayoutParams(
                        MapView.LayoutParams.WRAP_CONTENT,
                        MapView.LayoutParams.WRAP_CONTENT,
                        point,
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

            polygon.setOnClickListener((polygon1, mapView, eventPos) -> {
                // Consume tap events to prevent annoying speech bubble from
                // appearing when tapping areas overlaid with polygons
                return true;
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

            line.setOnClickListener((polyline, mapView, eventPos) -> {
                // Same as for polygons, consume tap events to prevent annoying speech bubbles
                //from appearing when lines are tapped
                return true;
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

    public String getCurrentFloor() {
        return currentFloor;
    }

    private void setCurrentFloor(String floor) {
        currentFloor = floor;
    }


    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        // ensure bounds/zoom are correct upon resuming
        enforceMapBounds();
        enforceZoomLimits();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }
}