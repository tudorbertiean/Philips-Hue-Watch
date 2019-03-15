package com.diplodesigns.huewatch.RoomSettings.Lights;

import android.app.Fragment;
import android.os.Bundle;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.diplodesigns.huewatch.R;
import com.diplodesigns.huewatch.RoomSettings.RoomSettingsActivity;
import com.philips.lighting.model.PHGroup;
import com.philips.lighting.model.PHLight;

import java.util.ArrayList;
import java.util.List;

public class LightsFragment extends Fragment {
    private LightAdapter adapter;
    private List<PHLight> allLights;
    private List<PHLight> roomLights = new ArrayList<>();
    private WearableListView lightsList;
    private PHGroup room;

    public LightsFragment(){
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_lights, container, false);

        room = RoomSettingsActivity.room;
        allLights = RoomSettingsActivity.allLights;
        lightsList = (WearableListView) rootView.findViewById(R.id.lightsListView);

        lightsList.setGreedyTouchMode(true);

        for (String lightID : room.getLightIdentifiers()){
            for (PHLight light : allLights) {
                if (lightID.equals(light.getIdentifier()))
                    roomLights.add(light);
            }
        }

        adapter = new LightAdapter(RoomSettingsActivity.context, roomLights);

        lightsList.setAdapter(adapter);

        try {
            lightsList.scrollToPosition(1);
        }catch (Exception e){
            e.printStackTrace();
        }

        return rootView;
    }
}
