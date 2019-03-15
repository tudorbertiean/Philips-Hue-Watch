package com.diplodesigns.huewatch.RoomSettings.Lights;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.diplodesigns.huewatch.Main.MainWatchActivity;
import com.diplodesigns.huewatch.R;
import com.philips.lighting.model.PHLight;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tudor on 3/31/2016.
 */
public final class LightAdapter extends WearableListView.Adapter {
    private static List<PHLight> lights;
    private static List<PHLight> lightsOn;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private static final String TAG = "RoomsAdapter";
    private static final String LIGHT_ON_PATH = "light-on";
    private static final String LIGHT_OFF_PATH = "light-off";

    // Provide a suitable constructor (depends on the kind of dataset)
    public LightAdapter(Context context, List<PHLight> lights) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.lights = lights;
        this.lightsOn = new ArrayList<>();
    }

    // Provide a reference to the type of views you're using
    public static class ItemViewHolder extends WearableListView.ViewHolder implements View.OnClickListener{
        private TextView lightName;
        private ImageView lightOn;

        public ItemViewHolder(View itemView) {
            super(itemView);
            // find the text view within the custom item's layout
            lightName = (TextView) itemView.findViewById(R.id.lightName);
            lightOn = (ImageView) itemView.findViewById(R.id.lightOn);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            //Update light state
            PHLight light = lights.get(getLayoutPosition());
            if (lightsOn.contains(light)){
                lightOn.setImageResource(R.drawable.room_item_off);
                lightsOn.remove(light);
                MainWatchActivity.sendMessage(light.getIdentifier(), LIGHT_OFF_PATH);
            }else{
                lightOn.setImageResource(R.drawable.room_item_on);
                lightsOn.add(light);
                MainWatchActivity.sendMessage(light.getIdentifier(), LIGHT_ON_PATH);
            }
        }
    }

    // Create new views for list items
    // (invoked by the WearableListView's layout manager)
    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        // Inflate our custom layout for list items
        return new ItemViewHolder(mInflater.inflate(R.layout.light_item, null));
    }

    // Replace the contents of a list item
    // Instead of creating new views, the list tries to recycle existing ones
    // (invoked by the WearableListView's layout manager)
    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        PHLight light = lights.get(position);
        TextView nameTxt = itemHolder.lightName;
        nameTxt.setText(light.getName());
        holder.itemView.setTag(position);

        if (lightsOn.contains(light)){
            itemHolder.lightOn.setImageResource(R.drawable.room_item_on);
        }else{
            itemHolder.lightOn.setImageResource(R.drawable.room_item_off);
        }
    }

    // Return the size of your dataset
    // (invoked by the WearableListView's layout manager)
    @Override
    public int getItemCount() {
        return lights.size();
    }
}
