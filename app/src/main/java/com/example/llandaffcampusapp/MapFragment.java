package com.example.llandaffcampusapp;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
    
    // Search components
    private EditText searchEditText;
    private ImageButton searchButton;
    private View searchResultsCard;
    private ListView searchResultsList;
    private ArrayAdapter<RoomSearchResult> searchResultsAdapter;
    private List<RoomSearchResult> roomSearchResults = new ArrayList<>();

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

        // Set up floor selection spinner
        Spinner floorSpinner = view.findViewById(R.id.floorSpinner);
        setupFloorSpinner(floorSpinner);
        
        // Set up legend button
        Button btnLegend = view.findViewById(R.id.btnLegend);
        btnLegend.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(view)
                .navigate(R.id.action_mapFragment_to_mapIndexFragment);
        });
        
        // Set up search functionality
        setupSearchFeature(view);

        // Loads ground floor by default
        loadFloorData("0");

        // Load the custom GeoJSON data and add its features to the map.
        JSONObject geoJson = GeoJsonUtils.loadGeoJsonFromAsset(getContext(), "campus_floor_0.geojson");
        addGeoJsonFeatures(geoJson);

        return view;
    }

    /**
     * Sets up the floor selection spinner with floor labels
     */
    private void setupFloorSpinner(Spinner spinner) {
        // Create an array of floor labels - more compact format
        String[] floors = new String[]{
                "0",
                "1",
                "2",
                "3",
                "4"
        };
        
        // Create the adapter and set it to the spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), 
                android.R.layout.simple_spinner_item,
                floors
        );
        
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        
        // Set listener for item selection
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // Extract the floor number based on position
                String floorNumber = String.valueOf(position);
                loadFloorData(floorNumber);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
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

        // Load the GeoJSON file for the selected floor
        String filename = "campus_floor_" + floorNumber + ".geojson";
        
        // Check if file exists before attempting to load
        JSONObject geoJson = GeoJsonUtils.loadGeoJsonFromAsset(getContext(), filename);
        
        if (geoJson != null) {
            addGeoJsonFeatures(geoJson);
            System.out.println("Loaded floor: " + floorNumber);
        } else {
            // If the file doesn't exist, show a message to the user
            System.out.println("Floor data not available for floor: " + floorNumber);
            showFloorUnavailableMessage();
        }
        
        mapView.invalidate();
    }

    /**
     * Displays a message when floor data isn't available
     */
    private void showFloorUnavailableMessage() {
        TextView message = new TextView(requireContext());
        message.setText("Floor data not available");
        message.setTextSize(18);
        message.setTextColor(Color.RED);
        message.setBackgroundColor(Color.WHITE);
        message.setPadding(20, 10, 20, 10);
        
        // Position the message at the center of the map
        GeoPoint center = (GeoPoint) mapView.getMapCenter();
        MapView.LayoutParams params = new MapView.LayoutParams(
                MapView.LayoutParams.WRAP_CONTENT,
                MapView.LayoutParams.WRAP_CONTENT,
                center,
                MapView.LayoutParams.CENTER,
                0, 0);
        
        mapView.addView(message, params);
    }

    private void addBuildingLabel(GeoPoint position, String buildingLetter) {
        TextView label = new TextView(requireContext());
        label.setText(buildingLetter);


        double currentZoom = mapView.getZoomLevelDouble();
        float textSize = (float)(currentZoom * 1); //text size
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
            
            // Define zoom thresholds for different levels of detail
            boolean showDetails = currentZoom >= 20.5;  // Only show points/icons at this zoom level
            boolean showRooms = currentZoom >= 19.5;    // Only show rooms at this zoom level or higher

            // Create lists to store features for ordered processing
            List<JSONObject> buildingFeatures = new ArrayList<>();
            List<JSONObject> roomFeatures = new ArrayList<>();
            List<JSONObject> roadFeatures = new ArrayList<>();
            List<JSONObject> otherFeatures = new ArrayList<>();
            
            // Debug - print number of features
            System.out.println("Total features: " + features.length());
            System.out.println("Current zoom: " + currentZoom + ", showing rooms: " + showRooms);
            
            // Categorize all features for proper layering
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONObject geometry = feature.getJSONObject("geometry");
                JSONObject properties = feature.optJSONObject("properties");
                String type = geometry.getString("type");

                // Skip building label points - process them immediately
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
                    continue;
                }

                // Categorize other features
                if ("LineString".equals(type) && 
                    properties != null && 
                    "service".equals(properties.optString("highway", ""))) {
                    // Roads go last
                    roadFeatures.add(feature);
                } else if (properties != null && "yes".equals(properties.optString("room", ""))) {
                    // Rooms - regardless of geometry type
                    roomFeatures.add(feature);
                    // Debug - print room properties
                    System.out.println("Found room with number: " + properties.optString("roomnumber", "unknown"));
                } else if ("Polygon".equals(type) && 
                           properties != null && 
                           "university".equals(properties.optString("building", ""))) {
                    // University buildings go first
                    buildingFeatures.add(feature);
                } else {
                    // Everything else in between
                    otherFeatures.add(feature);
                }
            }
            
            // Debug - print counts
            System.out.println("Building features: " + buildingFeatures.size());
            System.out.println("Room features: " + roomFeatures.size());
            System.out.println("Road features: " + roadFeatures.size());
            System.out.println("Other features: " + otherFeatures.size());
            
            // Process features in order of layering (bottom to top)
            
            // 1. Process building features first (bottom layer)
            for (JSONObject buildingFeature : buildingFeatures) {
                JSONObject geometry = buildingFeature.getJSONObject("geometry");
                JSONObject properties = buildingFeature.optJSONObject("properties");
                String type = geometry.getString("type");
                drawPolygon(geometry.getJSONArray("coordinates"), properties);
            }
            
            // 2. Process other features
            for (JSONObject otherFeature : otherFeatures) {
                JSONObject geometry = otherFeature.getJSONObject("geometry");
                JSONObject properties = otherFeature.optJSONObject("properties");
                String type = geometry.getString("type");
                
                switch (type) {
                    case "Point":
                        if (showDetails) {
                            drawPoint(geometry.getJSONArray("coordinates"), properties);
                        }
                        break;
                    case "LineString":
                        if (showDetails) {
                            drawLine(geometry.getJSONArray("coordinates"), properties);
                        }
                        break;
                    case "Polygon":
                        drawPolygon(geometry.getJSONArray("coordinates"), properties);
                        break;
                }
            }
            
            // 3. Process room features (on top of buildings but under roads) - only if zoomed in enough
            if (showRooms) {
                System.out.println("Drawing rooms at zoom level: " + currentZoom);
                for (JSONObject roomFeature : roomFeatures) {
                    JSONObject geometry = roomFeature.getJSONObject("geometry");
                    String type = geometry.getString("type");
                    JSONObject properties = roomFeature.optJSONObject("properties");
                    
                    // Debug - confirm we're drawing room
                    String roomNumber = properties.optString("roomnumber", "unknown");
                    System.out.println("Drawing room: " + roomNumber + " with geometry type: " + type);
                    
                    // Handle room based on its geometry type
                    if ("LineString".equals(type)) {
                        // Convert LineString to Polygon and draw as room
                        drawRoomFromLineString(geometry.getJSONArray("coordinates"), properties);
                    } else if ("Polygon".equals(type)) {
                        // Draw as usual
                        drawRoomPolygon(geometry.getJSONArray("coordinates"), properties);
                    }
                }
            } else {
                System.out.println("Not drawing rooms at zoom level: " + currentZoom);
            }
            
            // 4. Process road features last (top layer)
            for (JSONObject roadFeature : roadFeatures) {
                JSONObject geometry = roadFeature.getJSONObject("geometry");
                JSONObject properties = roadFeature.optJSONObject("properties");
                drawRoad(geometry.getJSONArray("coordinates"), properties);
            }
            
            mapView.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Converts a LineString to a Polygon and draws it as a room
     */
    private void drawRoomFromLineString(JSONArray coordinates, JSONObject properties) {
        try {
            // Create a list of GeoPoints from the LineString
            List<GeoPoint> points = new ArrayList<>();
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray point = coordinates.getJSONArray(i);
                double lon = point.getDouble(0);
                double lat = point.getDouble(1);
                points.add(new GeoPoint(lat, lon));
            }
            
            // Make sure the LineString is closed (first and last points match)
            if (points.size() > 2 && !points.get(0).equals(points.get(points.size() - 1))) {
                // Add the first point again to close the polygon
                points.add(new GeoPoint(points.get(0).getLatitude(), points.get(0).getLongitude()));
            }
            
            // Create a new polygon
            Polygon polygon = new Polygon(mapView);
            polygon.setPoints(points);
            
            // Set blue outline for rooms
            Paint outlinePaint = polygon.getOutlinePaint();
            outlinePaint.setColor(Color.BLUE);
            outlinePaint.setStrokeWidth(3f);
            
            // Set light blue fill for rooms
            Paint fillPaint = polygon.getFillPaint();
            fillPaint.setColor(Color.rgb(173, 216, 230)); // Light blue
            
            // Add to map
            mapView.getOverlays().add(polygon);
            
            // Get room number if available
            String roomNumber = properties.optString("roomnumber", "");
            double currentZoom = mapView.getZoomLevelDouble();
            if (!roomNumber.isEmpty() && currentZoom >= 20.5) {
                // Add room number label at center of room - only at highest zoom levels
                GeoPoint center = calculatePolygonCenter(points);
                TextView label = new TextView(requireContext());
                label.setText(roomNumber);
                label.setTextSize(10);
                label.setTextColor(Color.BLACK);
                label.setTypeface(null, Typeface.BOLD);
                label.setBackgroundColor(Color.TRANSPARENT);
                label.setPadding(4, 2, 4, 2);
                
                MapView.LayoutParams params = new MapView.LayoutParams(
                        MapView.LayoutParams.WRAP_CONTENT,
                        MapView.LayoutParams.WRAP_CONTENT,
                        center,
                        MapView.LayoutParams.CENTER,
                        0, 0);
                
                mapView.addView(label, params);
            }
            
            polygon.setOnClickListener((polygon1, mapView, eventPos) -> {
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Specialized method to draw room polygons with consistent styling
     */
    private void drawRoomPolygon(JSONArray coordinates, JSONObject properties) {
        try {
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

            // Set blue stroke for rooms
            Paint outlinePaint = polygon.getOutlinePaint();
            outlinePaint.setColor(Color.BLUE);
            outlinePaint.setStrokeWidth(3f);

            // Set light blue fill for rooms
            Paint fillPaint = polygon.getFillPaint();
            fillPaint.setColor(Color.rgb(173, 216, 230)); // Light blue

            // Add to map
            mapView.getOverlays().add(polygon);

            // Get room number if available
            String roomNumber = properties.optString("roomnumber", "");
            if (!roomNumber.isEmpty() && mapView.getZoomLevelDouble() >= 20.5) {
                // Add room number label at center of room
                GeoPoint center = calculatePolygonCenter(geoPoints);
                TextView label = new TextView(requireContext());
                label.setText(roomNumber);
                label.setTextSize(10);
                label.setTextColor(Color.BLACK);
                label.setTypeface(null, Typeface.BOLD);
                label.setBackgroundColor(Color.TRANSPARENT);
                label.setPadding(4, 2, 4, 2);
                
                MapView.LayoutParams params = new MapView.LayoutParams(
                        MapView.LayoutParams.WRAP_CONTENT,
                        MapView.LayoutParams.WRAP_CONTENT,
                        center,
                        MapView.LayoutParams.CENTER,
                        0, 0);
                
                mapView.addView(label, params);
            }

            polygon.setOnClickListener((polygon1, mapView, eventPos) -> {
                return true;
            });
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
            
            // Apply thicker stroke for university buildings
            if (properties != null && properties.optString("building", "").equals("university")) {
                outlinePaint.setStrokeWidth(detailed ? 4f : 2f); // Thicker for university buildings
            } else if (properties != null && "yes".equals(properties.optString("room", ""))) {
                outlinePaint.setStrokeWidth(detailed ? 3f : 1.5f); // Medium thickness for rooms
            } else {
                outlinePaint.setStrokeWidth(detailed ? 2f : 1f); // Normal thickness for other polygons
            }

            Paint fillPaint = polygon.getFillPaint();
            // Always apply fill color for rooms, regardless of zoom
            if (properties != null && "yes".equals(properties.optString("room", ""))) {
                fillPaint.setColor(Color.rgb(173, 216, 230)); // Light blue for rooms at all zoom levels
            } else {
                fillPaint.setColor(detailed ? getFillColor(properties) : Color.TRANSPARENT);
            }

            mapView.getOverlays().add(polygon);

            // Add a "P" label for parking areas
            if (properties != null && "parking".equals(properties.optString("amenity", ""))) {
                addParkingLabel(calculatePolygonCenter(geoPoints));
            }

            polygon.setOnClickListener((polygon1, mapView, eventPos) -> {
                // Consume tap events to prevent annoying speech bubble from
                // appearing when tapping areas overlaid with polygons
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Calculates the center point of a polygon
     */
    private GeoPoint calculatePolygonCenter(List<GeoPoint> points) {
        if (points == null || points.isEmpty()) {
            return null;
        }
        
        double latSum = 0;
        double lonSum = 0;
        
        for (GeoPoint point : points) {
            latSum += point.getLatitude();
            lonSum += point.getLongitude();
        }
        
        return new GeoPoint(latSum / points.size(), lonSum / points.size());
    }
    
    /**
     * Adds a "P" label at the center of a parking area
     */
    private void addParkingLabel(GeoPoint center) {
        if (center == null) return;
        
        // Get current zoom level to scale text size
        double zoomLevel = mapView.getZoomLevelDouble();
        
        TextView label = new TextView(requireContext());
        label.setText("P");
        
        // Dynamically calculate text size based on zoom level
        float baseSize = 8f; // Base size when at minimum zoom
        float zoomFactor = (float) (zoomLevel - MIN_ZOOM) / (float) (MAX_ZOOM - MIN_ZOOM);
        float textSize = baseSize + (16f * zoomFactor); // Scales from 8 to 24 across zoom range
        
        label.setTextSize(textSize);
        label.setTextColor(Color.BLUE);
        label.setTypeface(null, Typeface.BOLD);
        label.setBackgroundColor(Color.TRANSPARENT);
        label.setPadding(8, 4, 8, 4);
        
        MapView.LayoutParams params = new MapView.LayoutParams(
                MapView.LayoutParams.WRAP_CONTENT,
                MapView.LayoutParams.WRAP_CONTENT,
                center,
                MapView.LayoutParams.CENTER,
                0, 0);
        
        mapView.addView(label, params);
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
            paint.setColor(Color.rgb(0, 0, 0));
            paint.setStrokeWidth(4f);

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

    /**
     * Draws a road on the map
     */
    private void drawRoad(JSONArray coordinates, JSONObject properties) {
        try {
            Polyline road = new Polyline(mapView);
            List<GeoPoint> points = new ArrayList<>();

            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray point = coordinates.getJSONArray(i);
                double lon = point.getDouble(0);
                double lat = point.getDouble(1);
                points.add(new GeoPoint(lat, lon));
            }

            road.setPoints(points);
            
            // Set road style
            Paint paint = road.getOutlinePaint();
            paint.setColor(Color.DKGRAY); // Dark gray for roads
            paint.setStrokeWidth(10f);     // Thicker than regular lines
            paint.setStrokeCap(Paint.Cap.ROUND); // Rounded ends
            paint.setStrokeJoin(Paint.Join.ROUND); // Rounded corners
            
            // Get road width if specified
            float width = (float) properties.optDouble("width", 10.0);
            // Scale road width based on zoom level
            double zoomLevel = mapView.getZoomLevelDouble();
            width = (float)(width * (zoomLevel / 19.0)); // Adjust width based on zoom
            paint.setStrokeWidth(width);

            mapView.getOverlays().add(road);
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

        // Check if this is a room
        if (properties != null && "yes".equals(properties.optString("room", ""))) {
            return Color.rgb(173, 216, 230); // Light blue for rooms
        }

        // Check if this is a university building
        if (properties != null && properties.optString("building", "").equals("university")) {
            return Color.rgb(230, 220, 240); // Light purple beige for university buildings
        }
        
        // Check if this is a parking area
        if (properties != null && "parking".equals(properties.optString("amenity", ""))) {
            return Color.LTGRAY; // Light grey fill for parking areas
        }

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
        if (properties == null) return 0xFF000000;
        
        // Check if this is a room
        if (properties != null && "yes".equals(properties.optString("room", ""))) {
            return Color.BLUE; // Blue outline for rooms
        }
        
        // Check if this is a university building
        if (properties.optString("building", "").equals("university")) {
            return Color.BLUE; // Blue outline for university buildings
        }
        
        if (properties.has("strokeColor")) {
            String colorStr = properties.optString("strokeColor", "#00FF00");
            return parseColor(colorStr, 0xFF00FF00);
        }
        return 0xFF000000;
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

    /**
     * Sets up the room search feature
     */
    private void setupSearchFeature(View view) {
        // Initialize UI components
        searchEditText = view.findViewById(R.id.searchEditText);
        searchButton = view.findViewById(R.id.searchButton);
        searchResultsCard = view.findViewById(R.id.searchResultsCard);
        searchResultsList = view.findViewById(R.id.searchResultsList);
        
        // Create adapter for search results
        searchResultsAdapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_list_item_1, roomSearchResults);
        searchResultsList.setAdapter(searchResultsAdapter);
        
        // Set up search action listener
        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(searchEditText.getText().toString());
                return true;
            }
            return false;
        });
        
        // Set up search button click listener
        searchButton.setOnClickListener(v -> 
            performSearch(searchEditText.getText().toString())
        );
        
        // Set up text change listener
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 2) {
                    performSearch(s.toString());
                } else {
                    searchResultsCard.setVisibility(View.GONE);
                }
            }
        });
        
        // Set up result selection listener
        searchResultsList.setOnItemClickListener((parent, v, position, id) -> {
            RoomSearchResult selectedRoom = roomSearchResults.get(position);
            navigateToRoom(selectedRoom);
            
            // Hide results and clear search
            searchResultsCard.setVisibility(View.GONE);
        });
        
        // Click elsewhere to dismiss search results
        mapView.setOnClickListener(v -> searchResultsCard.setVisibility(View.GONE));
    }
    
    /**
     * Performs search and updates results UI
     */
    private void performSearch(String query) {
        if (query == null || query.trim().isEmpty()) {
            searchResultsCard.setVisibility(View.GONE);
            return;
        }
        
        // Convert to lowercase for case-insensitive search
        query = query.trim().toLowerCase();
        
        // Clear previous results
        roomSearchResults.clear();
        
        // Search for rooms in all available floor data
        List<RoomSearchResult> results = searchRoomsInAllFloors(query);
        
        if (results.isEmpty()) {
            // No results
            searchResultsCard.setVisibility(View.GONE);
            Toast.makeText(requireContext(), "No rooms found matching: " + query, Toast.LENGTH_SHORT).show();
        } else {
            // Update with new results
            roomSearchResults.addAll(results);
            searchResultsAdapter.notifyDataSetChanged();
            searchResultsCard.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * Searches for rooms in all available floor data
     */
    private List<RoomSearchResult> searchRoomsInAllFloors(String query) {
        List<RoomSearchResult> results = new ArrayList<>();
        
        // Check available floors (0-4)
        for (int floor = 0; floor <= 4; floor++) {
            String floorNumber = String.valueOf(floor);
            String filename = "campus_floor_" + floorNumber + ".geojson";
            
            // Only search floors with data available
            try {
                if (GeoJsonUtils.fileExists(requireContext(), filename)) {
                    JSONObject geoJson = GeoJsonUtils.loadGeoJsonFromAsset(requireContext(), filename);
                    if (geoJson != null) {
                        searchRoomsInFloor(geoJson, floorNumber, query, results);
                    }
                }
            } catch (Exception e) {
                Log.e("MapFragment", "Error searching floor " + floorNumber, e);
                // Continue searching other floors even if one fails
            }
        }
        
        Log.d("MapFragment", "Found " + results.size() + " matching rooms for query: " + query);
        return results;
    }
    
    /**
     * Searches for rooms in a specific floor's GeoJSON data
     */
    private void searchRoomsInFloor(JSONObject geoJson, String floorNumber, String query, List<RoomSearchResult> results) {
        try {
            JSONArray features = geoJson.getJSONArray("features");
            
            for (int i = 0; i < features.length(); i++) {
                try {
                    JSONObject feature = features.getJSONObject(i);
                    JSONObject properties = feature.optJSONObject("properties");
                    
                    if (properties != null && "yes".equals(properties.optString("room", ""))) {
                        String roomNumber = properties.optString("roomnumber", "");
                        
                        // Match if room number contains the query
                        if (!roomNumber.isEmpty() && roomNumber.toLowerCase().contains(query)) {
                            try {
                                // Extract coordinates for navigation
                                JSONObject geometry = feature.getJSONObject("geometry");
                                String geometryType = geometry.getString("type");
                                JSONArray coordinates = geometry.getJSONArray("coordinates");
                                
                                Log.d("MapFragment", "Found matching room: " + roomNumber + 
                                        " with geometry type: " + geometryType + 
                                        " on floor: " + floorNumber);
                                
                                // Handle different geometry types
                                RoomSearchResult roomResult = null;
                                
                                if ("LineString".equals(geometryType)) {
                                    // For LineString, we need to calculate the center of the line
                                    roomResult = extractRoomFromLineString(roomNumber, floorNumber, coordinates);
                                } else if ("Polygon".equals(geometryType)) {
                                    // For Polygon, we use the standard extraction method
                                    roomResult = extractRoomSearchResult(roomNumber, floorNumber, coordinates);
                                } else if ("Point".equals(geometryType)) {
                                    // For Point, the coordinates are a simple [lon, lat] pair
                                    try {
                                        double lon = coordinates.getDouble(0);
                                        double lat = coordinates.getDouble(1);
                                        roomResult = new RoomSearchResult(roomNumber, floorNumber, lat, lon);
                                    } catch (Exception e) {
                                        Log.e("MapFragment", "Error extracting Point coordinates", e);
                                    }
                                }
                                
                                if (roomResult != null) {
                                    results.add(roomResult);
                                }
                            } catch (Exception e) {
                                Log.e("MapFragment", "Error processing matching room " + roomNumber, e);
                                // Continue with next room even if this one fails
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e("MapFragment", "Error processing feature at index " + i, e);
                    // Continue with next feature even if this one fails
                }
            }
        } catch (Exception e) {
            Log.e("MapFragment", "Error searching rooms on floor " + floorNumber, e);
        }
    }
    
    /**
     * Extracts room search result from LineString coordinates
     */
    private RoomSearchResult extractRoomFromLineString(String roomNumber, String floorNumber, JSONArray coordinates) {
        try {
            // For LineString, coordinates should be an array of [lon, lat] points
            if (coordinates.length() < 2) {
                return null; // Need at least 2 points for a line
            }
            
            double sumLat = 0, sumLon = 0;
            int count = 0;
            
            // Calculate the center point by averaging all coordinates
            for (int i = 0; i < coordinates.length(); i++) {
                // Handle both possible formats: direct values or arrays
                if (coordinates.get(i) instanceof JSONArray) {
                    JSONArray point = coordinates.getJSONArray(i);
                    if (point.length() >= 2) {
                        sumLon += point.getDouble(0);
                        sumLat += point.getDouble(1);
                        count++;
                    }
                } else if (i == 0 && coordinates.length() >= 2) {
                    // Direct coordinate values [lon, lat]
                    double lon = coordinates.getDouble(0);
                    double lat = coordinates.getDouble(1);
                    return new RoomSearchResult(roomNumber, floorNumber, lat, lon);
                }
            }
            
            if (count > 0) {
                double centerLat = sumLat / count;
                double centerLon = sumLon / count;
                return new RoomSearchResult(roomNumber, floorNumber, centerLat, centerLon);
            }
        } catch (Exception e) {
            Log.e("MapFragment", "Error extracting LineString room coordinates", e);
        }
        return null;
    }
    
    /**
     * Extracts room search result with coordinates from GeoJSON geometry
     */
    private RoomSearchResult extractRoomSearchResult(String roomNumber, String floorNumber, JSONArray coordinates) {
        try {
            double lat = 0, lon = 0;
            int pointCount = 0;
            
            // Log coordinate structure for debugging
            Log.d("MapFragment", "Room coordinates structure: " + coordinates.toString());
            
            // Different GeoJSON features can have different coordinate structures
            // Handle this specific structure that caused the error
            if (coordinates.length() > 0) {
                // Handle case where first element is a direct coordinate value (not an array)
                if (!(coordinates.get(0) instanceof JSONArray)) {
                    // This is likely a special case where direct longitude values are provided
                    // Check if we have at least two values for a coordinate pair
                    if (coordinates.length() >= 2) {
                        try {
                            // Try to get the values directly
                            lon = coordinates.getDouble(0);
                            lat = coordinates.getDouble(1);
                            return new RoomSearchResult(roomNumber, floorNumber, lat, lon);
                        } catch (Exception e) {
                            Log.e("MapFragment", "Error extracting direct coordinates", e);
                        }
                    }
                    return null;
                }
                
                JSONArray points;
                
                // Handle different geometry types
                if (coordinates.get(0) instanceof JSONArray) {
                    // Check if this is a polygon with inner arrays
                    if (coordinates.getJSONArray(0).get(0) instanceof JSONArray) {
                        // Polygon coordinates: [[[lon,lat],[lon,lat],...]]
                        points = coordinates.getJSONArray(0);
                    } else {
                        // LineString coordinates: [[lon,lat],[lon,lat],...]
                        points = coordinates;
                    }
                    
                    // Average coordinates to find center
                    for (int i = 0; i < points.length(); i++) {
                        JSONArray point = points.getJSONArray(i);
                        if (point.length() >= 2) {
                            lon += point.getDouble(0);
                            lat += point.getDouble(1);
                            pointCount++;
                        }
                    }
                    
                    if (pointCount > 0) {
                        lat /= pointCount;
                        lon /= pointCount;
                        return new RoomSearchResult(roomNumber, floorNumber, lat, lon);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("MapFragment", "Error extracting room coordinates", e);
        }
        
        // If we couldn't extract coordinates, return null
        return null;
    }
    
    /**
     * Navigates to the selected room
     */
    private void navigateToRoom(RoomSearchResult room) {
        // Switch to the correct floor
        if (!getCurrentFloor().equals(room.floorNumber)) {
            loadFloorData(room.floorNumber);
            
            // Update spinner to match the floor
            Spinner floorSpinner = requireView().findViewById(R.id.floorSpinner);
            floorSpinner.setSelection(Integer.parseInt(room.floorNumber));
        }
        
        // Animate to room location
        GeoPoint roomPoint = new GeoPoint(room.latitude, room.longitude);
        mapView.getController().animateTo(roomPoint);
        
        // Zoom in to appropriate level if needed
        if (mapView.getZoomLevelDouble() < 20.0) {
            mapView.getController().setZoom(20.0);
        }
        
        // Add a temporary marker at the room location
        addTemporaryRoomMarker(roomPoint, room.roomNumber);
        
        Toast.makeText(requireContext(), "Navigating to room " + room.roomNumber, Toast.LENGTH_SHORT).show();
    }
    
    /**
     * Adds a temporary highlight marker for the found room
     */
    private void addTemporaryRoomMarker(GeoPoint point, String roomNumber) {
        // Create a marker
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle("Room " + roomNumber);
        marker.setSnippet("Tap to dismiss");
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        
        // Use a standard marker icon with a custom color
        Drawable icon = getResources().getDrawable(org.osmdroid.library.R.drawable.marker_default);
        marker.setIcon(icon);
        
        // Add to map
        mapView.getOverlays().add(marker);
        
        // Make the info window appear immediately
        marker.showInfoWindow();
        
        // Set up auto-dismiss after a few seconds
        handler.postDelayed(() -> {
            if (marker != null && mapView != null && mapView.getOverlays().contains(marker)) {
                mapView.getOverlays().remove(marker);
                mapView.invalidate();
            }
        }, 5000); // Remove after 5 seconds
        
        // Invalidate to update display
        mapView.invalidate();
    }
    
    /**
     * Class representing a room search result with location data for navigation
     */
    private static class RoomSearchResult {
        final String roomNumber;
        final String floorNumber;
        final double latitude;
        final double longitude;
        
        RoomSearchResult(String roomNumber, String floorNumber, double latitude, double longitude) {
            this.roomNumber = roomNumber;
            this.floorNumber = floorNumber;
            this.latitude = latitude;
            this.longitude = longitude;
        }
        
        @Override
        public String toString() {
            return roomNumber + " (Floor " + floorNumber + ")";
        }
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