package com.diplodesigns.huewatch.RoomSettings;

import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.diplodesigns.huewatch.R;

public class BrightnessFragment extends Fragment {
    private SeekBar dimBar;
    private TextView roomName;
    private DimAdapter adapter;
    private WearableListView dimView;

    public BrightnessFragment(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_brightness, container, false);

        roomName = (TextView) rootView.findViewById(R.id.roomNameText);
        dimView = (WearableListView) rootView.findViewById(R.id.brightnessView);

        adapter = new DimAdapter(RoomSettingsActivity.context);
        dimView.setAdapter(adapter);
        dimView.setGreedyTouchMode(true);

        roomName.setText(RoomSettingsActivity.room.getName());

        return rootView;
    }
}
