package com.example.llandaffcampusapp1;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 *fragment showing index of map icons + descriptions
 */
public class MapIndexFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map_index, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //init close button
        view.findViewById(R.id.btnClose).setOnClickListener(v -> {
            //on click, go back to map frag
            requireActivity().onBackPressed();
        });
        
        //init table layout
        TableLayout legendTable = view.findViewById(R.id.legend_table);

        //def map icons and descriptions
        //LinkedHashMap maintains insert order
        Map<String, IconInfo> mapIcons = new LinkedHashMap<>();
        mapIcons.put("information", new IconInfo(R.drawable.ic_inf_desk, "Information Desk"));
        mapIcons.put("library", new IconInfo(R.drawable.ic_library2, "Library"));
        mapIcons.put("stairs", new IconInfo(R.drawable.ic_stairs, "Stairs"));
        mapIcons.put("food", new IconInfo(R.drawable.ic_food, "Food Court/Restaurant"));
        mapIcons.put("computer", new IconInfo(R.drawable.ic_computer, "Computer Lab"));
        mapIcons.put("toilet", new IconInfo(R.drawable.ic_toilet, "Restroom/Toilet"));
        mapIcons.put("accessible", new IconInfo(R.drawable.ic_accessible, "Accessible Facilities"));
        mapIcons.put("coffee", new IconInfo(R.drawable.ic_coffee, "Coffee Shop"));
        mapIcons.put("table", new IconInfo(R.drawable.ic_tables, "Study Tables"));
        mapIcons.put("elevator", new IconInfo(R.drawable.ic_elevator, "Elevator"));
        mapIcons.put("gym", new IconInfo(R.drawable.ic_gym, "Gym/Fitness Center"));
        mapIcons.put("it", new IconInfo(R.drawable.ic_it, "IT Support"));
        mapIcons.put("parking", new IconInfo(R.drawable.ic_parking, "Parking"));
        mapIcons.put("lecture", new IconInfo(R.drawable.ic_lecture, "Lecture Hall"));

        //add icons to table
        for (Map.Entry<String, IconInfo> entry : mapIcons.entrySet()) {
            IconInfo iconInfo = entry.getValue();
            
            //create new row
            TableRow row = new TableRow(requireContext());
            row.setPadding(0, 8, 0, 8);
            
            //create icons with size of 35dp
            ImageView iconView = new ImageView(requireContext());
            iconView.setImageResource(iconInfo.iconResId);
            
            //convert 35dp to pixels
            int iconSizeInDp = 35;
            float scale = getResources().getDisplayMetrics().density;
            int iconSizeInPx = (int) (iconSizeInDp * scale + 0.5f);
            
            TableRow.LayoutParams iconParams = new TableRow.LayoutParams(
                    iconSizeInPx,
                    iconSizeInPx);
            iconParams.setMargins(0, 0, 32, 0);
            iconView.setLayoutParams(iconParams);
            
            //without this icons scale wrong when set to 35dp
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            
            //add & conf description text
            TextView descText = new TextView(requireContext());
            descText.setText(iconInfo.description);
            descText.setTextSize(16);
            
            //add text and icon to row
            row.addView(iconView);
            row.addView(descText);
            
            //add row to table
            legendTable.addView(row);
        }
    }

    /**
     *class to hold icon resource ID and description
     */
    private static class IconInfo {
        final int iconResId;
        final String description;

        IconInfo(int iconResId, String description) {
            this.iconResId = iconResId;
            this.description = description;
        }
    }
}