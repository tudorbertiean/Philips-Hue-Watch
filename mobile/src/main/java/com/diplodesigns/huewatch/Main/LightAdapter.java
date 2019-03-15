package com.diplodesigns.huewatch.Main;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.diplodesigns.huewatch.PhilipsHue.PHRoom;
import com.diplodesigns.huewatch.R;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tudor on 3/29/2016.
 */
public class LightAdapter extends RecyclerView.Adapter<LightAdapter.LightViewHolder>  {
    private PHBridge bridge;
    private List<PHLight> allLights; //All lights that will be filtered to room lights
    public static List<PHLight> roomLights; //Filtered from allLights
    private List<PHLight> copyOfOrigLights;
    private PHRoom room;
    private Boolean isEdit;
    private Boolean isNew;

    public LightAdapter(PHBridge bridge, PHRoom room, Boolean isEdit, Boolean isNew){
        this.bridge = bridge;
        this.allLights = bridge.getResourceCache().getAllLights();
        this.room = room;
        this.isEdit = isEdit;
        this.isNew = isNew;
        this.roomLights = new ArrayList<>();

        if (room.getGroup().getLightIdentifiers() != null){ //Skip if this is a new room being added
            for (String lightID : room.getGroup().getLightIdentifiers()) {
                for (PHLight light : allLights) {
                    if (lightID.equals(light.getIdentifier())) {
                        roomLights.add(light);
                        break;
                    }
                }
            }
        }

        if(isEdit && !isNew) {
            this.copyOfOrigLights = new ArrayList<>();
            for (PHLight light : roomLights)
                copyOfOrigLights.add(light);
        }
    }

    @Override
    public LightViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.light_item, parent, false);

        return new LightViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(LightViewHolder holder, int position) {
        PHLight light;
        if (!isEdit) { //Get the lights from only roomLights list
            light = roomLights.get(position);
            if (light.supportsColor()){
                holder.colorLightImg.setImageResource(R.mipmap.color_light);
            }else{
                holder.colorLightImg.setImageResource(R.mipmap.white_light);
            }

            holder.lightCheckBox.setVisibility(View.INVISIBLE);
            if (light.getLastKnownLightState().isReachable()) {
                holder.lightSwitch.setVisibility(View.VISIBLE);
                if (light.getLastKnownLightState().isOn()) {
                    holder.lightSwitch.setChecked(true);
                } else {
                    holder.lightSwitch.setChecked(false);
                }
            }else{
                holder.lightUnreachableImg.setVisibility(View.VISIBLE);
                holder.lightSwitch.setVisibility(View.INVISIBLE);
            }
        }
        else { //Get lights from allLights list
            light = allLights.get(position);
            if (light.supportsColor()){
                holder.colorLightImg.setImageResource(R.mipmap.color_light);
            }else{
                holder.colorLightImg.setImageResource(R.mipmap.white_light);
            }

            holder.lightUnreachableImg.setVisibility(View.INVISIBLE);
            holder.lightCheckBox.setVisibility(View.VISIBLE);
            holder.lightSwitch.setVisibility(View.INVISIBLE);
            if (roomLights.contains(light)){
                holder.lightCheckBox.setChecked(true);
            }else{
                holder.lightCheckBox.setChecked(false);
            }
        }

        holder.lightName.setText(light.getName());
    }

    @Override
    public int getItemCount() {
        if (!isEdit)
            return roomLights.size();
        else
            return allLights.size();
    }

    public class LightViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
        public TextView lightName;
        public Switch lightSwitch;
        public CheckBox lightCheckBox;
        public ImageView lightUnreachableImg;
        public ImageView colorLightImg;

        LightViewHolder(View itemView) {
            super(itemView);
            lightName = (TextView) itemView.findViewById(R.id.lightNametext);
            lightSwitch = (Switch) itemView.findViewById(R.id.lightSwitch);
            lightCheckBox = (CheckBox) itemView.findViewById(R.id.lightSelectBox);
            lightUnreachableImg = (ImageView) itemView.findViewById(R.id.lightUnreachableImg);
            colorLightImg = (ImageView) itemView.findViewById(R.id.colorLightImg);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (!isEdit) {
                PHLight selectedLight = roomLights.get(getLayoutPosition());
                PHLightState state = selectedLight.getLastKnownLightState();
                if (state.isReachable()) {
                    if (lightSwitch.isChecked()) {
                        lightSwitch.setChecked(false);
                        state.setOn(false);
                        bridge.updateLightState(selectedLight, state);
                    } else {
                        lightSwitch.setChecked(true);
                        state.setOn(true);
                        bridge.updateLightState(selectedLight, state);
                    }
                }
            }else{
                PHLight selectedLight = allLights.get(getLayoutPosition());
                if (lightCheckBox.isChecked()) {
                    lightCheckBox.setChecked(false);
                    roomLights.remove(selectedLight);
                }else{
                    lightCheckBox.setChecked(true);
                    roomLights.add(selectedLight);
                }

                //Change from 'Cancel' to 'Save' because user has at least one light selected now
                if (roomLights.size() > 0 && isNew
                        || isEdit && !isNew && !roomLights.containsAll(copyOfOrigLights)
                        || isEdit && !isNew && roomLights.size() != copyOfOrigLights.size()
                        || isEdit && !isNew && !room.getGroup().getName().equals(RoomsActivity.roomName.getText().toString())){

                    RoomsActivity.dismissText.setText("Save");
                    RoomsActivity.dismissText.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            RoomsActivity.saveRoom(isNew);
                        }
                    });
                }else{
                    RoomsActivity.dismissText.setText("Cancel");
                    RoomsActivity.dismissText.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (isNew)
                                RoomsActivity.roomDialog.dismiss();
                            else {
                                RoomsActivity.setDismissDialog();
                            }
                        }
                    });
                }
            }
        }
    }
}
