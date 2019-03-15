package com.diplodesigns.huewatch.Main;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.diplodesigns.huewatch.R;
import com.philips.lighting.model.PHGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tudor on 3/31/2016.
 */
public final class RoomsAdapter extends WearableListView.Adapter {
    private static List<PHGroup> rooms;
    private static List<PHGroup> roomsOn;
    private final Context mContext;
    private final LayoutInflater mInflater;
    private static final String TAG = "RoomsAdapter";
    private static final String ROOM_ON_PATH = "room-on";
    private static final String ROOM_OFF_PATH = "room-off";

    // Provide a suitable constructor (depends on the kind of dataset)
    public RoomsAdapter(Context context, List<PHGroup> rooms) {
        this.mContext = context;
        this.mInflater = LayoutInflater.from(context);
        this.roomsOn = new ArrayList<>();
        this.rooms = rooms;
    }

    // Provide a reference to the type of views you're using
    public static class ItemViewHolder extends WearableListView.ViewHolder implements View.OnLongClickListener, View.OnClickListener{
        private TextView roomName;
        private ImageView roomState;

        public ItemViewHolder(View itemView) {
            super(itemView);
            // find the text view within the custom item's layout
            roomName = (TextView) itemView.findViewById(R.id.roomName);
            roomState = (ImageView) itemView.findViewById(R.id.roomState);

            itemView.setOnLongClickListener(this);
            itemView.setOnClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            MainWatchActivity.startRoomActivity(rooms.get(getLayoutPosition()));
            return true;
        }

        @Override
        public void onClick(View v) {
            //Update light state
            PHGroup room = rooms.get(getLayoutPosition());
            if (roomsOn.contains(room)){
                roomState.setImageResource(R.drawable.room_item_off);
                roomsOn.remove(room);
                MainWatchActivity.sendMessage(room.getIdentifier(), ROOM_OFF_PATH);
            }else{
                roomState.setImageResource(R.drawable.room_item_on);
                roomsOn.add(room);
                MainWatchActivity.sendMessage(room.getIdentifier(), ROOM_ON_PATH);
            }
        }
    }

    // Create new views for list items
    // (invoked by the WearableListView's layout manager)
    @Override
    public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        // Inflate our custom layout for list items
        return new ItemViewHolder(mInflater.inflate(R.layout.room_item, null));
    }

    // Replace the contents of a list item
    // Instead of creating new views, the list tries to recycle existing ones
    // (invoked by the WearableListView's layout manager)
    @Override
    public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        PHGroup room = rooms.get(position);
        TextView nameTxt = itemHolder.roomName;
        nameTxt.setText(room.getName() + " (" + room.getLightIdentifiers().size() + ")");
        holder.itemView.setTag(position);

        if (roomsOn.contains(room)){
            itemHolder.roomState.setImageResource(R.drawable.room_item_on);
        }else{
            itemHolder.roomState.setImageResource(R.drawable.room_item_off);
        }

    }

    // Return the size of your dataset
    // (invoked by the WearableListView's layout manager)
    @Override
    public int getItemCount() {
        return rooms.size();
    }
}
