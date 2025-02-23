package com.example.llandaffcampusapp1;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;

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
    private Spinner floorSpinner;
    private List<Searchable> searchables = new ArrayList<>();

    private int currentFloor = 0;

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

        // Set up the Spinner
        floorSpinner = view.findViewById(R.id.floorSpinner);

        // Create an ArrayAdapter using the floor_array
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.floor_array,
                R.layout.spinner
        );
        adapter.setDropDownViewResource(R.layout.spinner);
        floorSpinner.setAdapter(adapter);

        // Listen for floor selection
        floorSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View itemView, int position, long id) {
                // position 0 -> Floor 0, 1 -> Floor 1, etc.
                // Convert to a string or use position directly
                // For example, if you load separate files for each floor:
                loadFloorData(String.valueOf(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed if nothing is selected
            }
        });


        // Loads ground floor by default
        loadFloorData("0");

        // Load the custom GeoJSON data and add its features to the map.
        JSONObject geoJson = GeoJsonUtils.loadGeoJsonFromAsset(getContext(), "campus_floor_0.geojson");
        addGeoJsonFeatures(geoJson);

        AutoCompleteTextView searchBox = view.findViewById(R.id.searchBox);

        // Create a simple list of names for the adapter
        List<String> allNames = new ArrayList<>();
        for (Searchable sf : searchables) {
            allNames.add(sf.name); // or combine with tags if you want
        }

        // If you have duplicates, consider using a Set or removing duplicates
        ArrayAdapter<String> searchAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                allNames
        );

        searchBox.setAdapter(searchAdapter);
        searchBox.setOnItemClickListener((parent, itemView, position, id) -> {
            String selectedName = (String) parent.getItemAtPosition(position);
            // Find the matching SearchableFeature
            for (Searchable sf : searchables) {
                if (sf.name.equals(selectedName)) {
                    focusOnFeature(sf);
                    break;
                }
            }
        });

        return view;
    }

    private void focusOnFeature(Searchable sf) {

        int featureFloor = Integer.parseInt(sf.floor);

        if (featureFloor != currentFloor) {
            setFloor(featureFloor);
        }

        // 1. Switch floor if needed
        if (!sf.floor.equals(String.valueOf(currentFloor))) {
            setFloor(Integer.parseInt(sf.floor));
        }

        // 2. Zoom/center on the feature
        mapView.getController().animateTo(sf.center, 19.0, 1200L);
        // e.g., zoom level 19, over 1200ms, adjust as needed

        // 3. Optionally highlight or show info
        if (sf.overlay instanceof Marker) {
            Marker m = (Marker) sf.overlay;
            m.showInfoWindow();
        } else if (sf.overlay instanceof Polygon) {
            // could change fill color temporarily or show an InfoWindow
            // e.g., polygon.showInfoWindow()
        }
    }

    private void setFloor(int floor) {
        currentFloor = floor;
        mapView.getOverlays().clear();

        // load the correct file
        String filename = "campus_floor_" + floor + ".geojson";
        JSONObject geoJson = GeoJsonUtils.loadGeoJsonFromAsset(getContext(), filename);
        addGeoJsonFeatures(geoJson);

        mapView.invalidate();
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

                String floorVal = properties != null ? properties.optString("floor", "0") : "0";

                switch (type) {
                    case "Point":
                        drawPoint(geometry.getJSONArray("coordinates"), properties, floorVal);
                        break;

                    case "Polygon":
                        drawPolygon(geometry.getJSONArray("coordinates"), properties, floorVal);
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
    private void drawPoint(JSONArray coords, JSONObject properties, String floor) {
        try {
            double lon = coords.getDouble(0);
            double lat = coords.getDouble(1);

            Marker marker = new Marker(mapView);
            marker.setPosition(new GeoPoint(lat, lon));

            String name = properties.optString("name", "Unknown");
            marker.setTitle(name);

            // Read the "icon" or "icon:url" property from GeoJSON
            String iconType = properties != null ? properties.optString("icon", null) : null;
            if (iconType != null) {
                int iconResId = getIconResource(iconType);
                if (iconResId != 0) {
                    marker.setIcon(ContextCompat.getDrawable(requireContext(), iconResId));
                }
            }

            String tagStr = properties.optString("tags", "");
            String[] tags = tagStr.isEmpty() ? new String[0] : tagStr.split(";");

            Searchable sf = new Searchable(
                    name,
                    tags,
                    floor,
                    marker,
                    marker.getPosition()  // center is the marker's position
            );
            searchables.add(sf);

            // Title, snippet, etc.
            marker.setTitle(properties.optString("name", "No Name"));
            mapView.getOverlays().add(marker);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void drawPolygon(JSONArray coordinates, JSONObject properties, String floor) {
        try {
            // coordinates[0] is typically the outer ring. Additional rings are holes.
            JSONArray outerRing = coordinates.getJSONArray(0);
            Polygon polygon = new Polygon(mapView);

            GeoPoint center = computePolygonCenter(coordinates);

            String name = properties.optString("name", "Unknown");
            String tagStr = properties.optString("tags", "");
            String[] tags = tagStr.isEmpty() ? new String[0] : tagStr.split(";");

            Searchable sf = new Searchable(
                    name,
                    tags,
                    floor,
                    polygon,
                    center
            );
            searchables.add(sf);

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

            // Show label when tapped
            name = properties != null ? properties.optString("name", "No Name") : "No Name";
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

    private GeoPoint computePolygonCenter(JSONArray coordinates) {
        // Very rough example: just take average lat/lon of outer ring
        try {
            JSONArray outerRing = coordinates.getJSONArray(0);
            double sumLat = 0;
            double sumLon = 0;
            for (int i = 0; i < outerRing.length(); i++) {
                JSONArray point = outerRing.getJSONArray(i);
                double lon = point.getDouble(0);
                double lat = point.getDouble(1);
                sumLat += lat;
                sumLon += lon;
            }
            double count = outerRing.length();
            return new GeoPoint(sumLat / count, sumLon / count);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new GeoPoint(0,0); // fallback
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
