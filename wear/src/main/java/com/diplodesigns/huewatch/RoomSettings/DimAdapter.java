package com.diplodesigns.huewatch.RoomSettings;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import com.diplodesigns.huewatch.Main.MainWatchActivity;
import com.diplodesigns.huewatch.R;

/**
 * Created by Tudor on 3/31/2016.
 */
public final class DimAdapter extends WearableListView.Adapter {
    private final Context mContext;
    private final LayoutInflater mInflater;
    private static final String ROOM_DIM_PATH = "room-dim";

    // Provide a suitable constructor (depends on the kind of dataset)
    public DimAdapter(Context context) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
    }

    // Provide a reference to the type of views you're using
    public static class ItemViewHolder extends WearableListView.ViewHolder{
        private SeekBar roomDim;

        public ItemViewHolder(View itemView) {
            super(itemView);
            // find the text view within the custom item's layout
            roomDim = (SeekBar) itemView.findViewById(R.id.dimnessProgress);
        }
    }

    // Create new views for list items
    // (invoked by the WearableListView's layout manager)
    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        // Inflate our custom layout for list items
        return new ItemViewHolder(mInflater.inflate(R.layout.brightness_slider, null));
    }

    // Replace the contents of a list item
    // Instead of creating new views, the list tries to recycle existing ones
    // (invoked by the WearableListView's layout manager)
    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        ItemViewHolder itemHolder = (ItemViewHolder) holder;

        itemHolder.roomDim.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MainWatchActivity.sendMessage(RoomSettingsActivity.room.getIdentifier() + "," + seekBar.getProgress(), ROOM_DIM_PATH);
            }
        });
    }

    // Return the size of your dataset
    // (invoked by the WearableListView's layout manager)
    @Override
    public int getItemCount() {
        return 1;
    }
}
