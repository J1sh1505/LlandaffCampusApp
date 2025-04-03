package com.example.llandaffcampusapp;

import android.graphics.Color;
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

import java.util.ArrayList;
import java.util.List;

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
            //on click, go back to map fragment
            requireActivity().onBackPressed();
        });
        
        //init table layout
        TableLayout legendTable = view.findViewById(R.id.legend_table);

        //def map icons and descriptions
        List<IconInfo> mapIcons = new ArrayList<>();
        mapIcons.add(new IconInfo(R.drawable.ic_inf_desk, R.string.legend_info_desk));
        mapIcons.add(new IconInfo(R.drawable.ic_library2, R.string.legend_library));
        mapIcons.add(new IconInfo(R.drawable.ic_stairs, R.string.legend_stairs));
        mapIcons.add(new IconInfo(R.drawable.ic_food, R.string.legend_food));
        mapIcons.add(new IconInfo(R.drawable.ic_computer, R.string.legend_computer));
        mapIcons.add(new IconInfo(R.drawable.ic_toilet, R.string.legend_toilet));
        mapIcons.add(new IconInfo(R.drawable.ic_accessible, R.string.legend_accessible));
        mapIcons.add(new IconInfo(R.drawable.ic_coffee, R.string.legend_coffee));
        mapIcons.add(new IconInfo(R.drawable.ic_tables, R.string.legend_tables));
        mapIcons.add(new IconInfo(R.drawable.ic_elevator, R.string.legend_elevator));
        mapIcons.add(new IconInfo(R.drawable.ic_gym, R.string.legend_gym));
        mapIcons.add(new IconInfo(R.drawable.ic_it, R.string.legend_it));
        mapIcons.add(new IconInfo(R.drawable.ic_parking, R.string.legend_parking));
        mapIcons.add(new IconInfo(R.drawable.ic_lecture, R.string.legend_lecture));

        //add icons to table
        for (IconInfo iconInfo : mapIcons) {
            //make new row
            TableRow row = new TableRow(requireContext());
            row.setPadding(0, 12, 0, 12);
            
            //create icons with size of 40dp
            ImageView iconView = new ImageView(requireContext());
            iconView.setImageResource(iconInfo.iconResId);

            //set icon colour to off white for better visibility
            iconView.setColorFilter(Color.parseColor("#F5F5F5"), android.graphics.PorterDuff.Mode.SRC_IN);


            //convert 40dp to pixels
            int iconSizeInDp = 40;
            float scale = getResources().getDisplayMetrics().density;
            int iconSizeInPx = (int) (iconSizeInDp * scale + 0.5f);
            
            TableRow.LayoutParams iconParams = new TableRow.LayoutParams(
                    iconSizeInPx,
                    iconSizeInPx);
            iconParams.setMargins(0, 0, 32, 0);
            iconView.setLayoutParams(iconParams);
            
            //w/o this icons scale wrong when set to specific dp
            iconView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            
            //add & config description text
            TextView descText = new TextView(requireContext());
            descText.setText(iconInfo.descriptionResId);
            descText.setTextSize(16);
            descText.setTextColor(Color.WHITE);
            
            //add text and icon to row
            row.addView(iconView);
            row.addView(descText);
            
            //add row to table
            legendTable.addView(row);
            
            //add dividers between rows
            if (mapIcons.indexOf(iconInfo) < mapIcons.size() - 1) {
                View divider = new View(requireContext());
                TableLayout.LayoutParams dividerParams = new TableLayout.LayoutParams(
                        TableLayout.LayoutParams.MATCH_PARENT, 
                        1); //1px height
                dividerParams.setMargins(0, 8, 0, 8);
                divider.setLayoutParams(dividerParams);
                divider.setBackgroundColor(Color.parseColor("#4DFFFFFF")); // Semi-transparent white
                legendTable.addView(divider);
            }
        }
    }

    /**
     *class to hold icon resource ID and description
     */
    private static class IconInfo {
        final int iconResId;
        final int descriptionResId;

        IconInfo(int iconResId, int descriptionResId) {
            this.iconResId = iconResId;
            this.descriptionResId = descriptionResId;
        }
    }
}