package com.diplodesigns.huewatch;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.philips.lighting.model.PHLight;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by Tudor on 3/28/2016.
 */
public class RoomsAdapter extends RecyclerView.Adapter<RoomsAdapter.RoomViewHolder> {
    public static Boolean deleteMode;
    private List<PHRoom> rooms;
    private HueController hueController;
    public static List<PHRoom> roomsToDelete;
    private Map<String, PHLight> allLights;
    private boolean oneLightReachable;
    private boolean allLightsOn;

    public RoomsAdapter(List<PHRoom> rooms, HueController hueController){
        this.rooms = rooms;
        this.hueController = hueController;
        this.roomsToDelete = new ArrayList<>();
        this.deleteMode = false;
    }

    @Override
    public RoomViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.room_item, parent, false);

        return new RoomViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RoomViewHolder holder, int position) {
        PHRoom room = rooms.get(position);
        holder.roomName.setText(room.getGroup().getName());

        if (!deleteMode) {
            if (room.getIsReachable()) {
                holder.lightCount.setVisibility(View.VISIBLE);
                holder.groupsUnreachable.setVisibility(View.INVISIBLE);
                holder.lightCount.setText(Integer.toString(room.getGroup().getLightIdentifiers().size()));
            }else{
                holder.lightCount.setVisibility(View.INVISIBLE);
                holder.groupsUnreachable.setVisibility(View.VISIBLE);
            }
            if (room.getIsOn()) {
                holder.background.setBackgroundResource(R.drawable.room_item_on);
            }
            else
                holder.background.setBackgroundResource(R.drawable.room_item_off);
        }
        else{
            if (position != 0)
                holder.lightCount.setText("");
            if (roomsToDelete.contains(room)){
                holder.background.setBackgroundResource(R.drawable.room_item_delete);
            }else{
                holder.background.setBackgroundResource(R.drawable.room_item_off);
            }
        }
    }

    @Override
    public int getItemCount() {
        return rooms.size();
    }

    public class RoomViewHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener, View.OnClickListener {
        public TextView roomName, lightCount;
        public RelativeLayout background;
        public ImageView groupsUnreachable;

        RoomViewHolder(View itemView) {
            super(itemView);
            roomName = (TextView) itemView.findViewById(R.id.roomNameTxt);
            lightCount = (TextView) itemView.findViewById(R.id.lightCountTxt);
            background = (RelativeLayout) itemView.findViewById(R.id.roomBackground);
            groupsUnreachable = (ImageView) itemView.findViewById(R.id.groupsUnreachableImg);

            itemView.setOnClickListener(this);
            itemView.setOnLongClickListener(this);
        }

        @Override
        public boolean onLongClick(View v) {
            if (!deleteMode) {
                if (getLayoutPosition() != 0)
                    RoomsActivity.showRoomDialog(rooms.get(getLayoutPosition()), false, false);
                else
                    onClick(v);
            }
            return true;
        }

        @Override
        public void onClick(View v) {
            Log.wtf("RoomsAdapter", "onClick");
            //Check if on or off and animate accordingly
            PHRoom selectedRoom = rooms.get(getLayoutPosition());
            if (!deleteMode) {
                if (selectedRoom.getIsReachable()) {
                    boolean isOn = selectedRoom.getIsOn();
                    if (isOn) {
                        selectedRoom.setIsOn(false);
                    } else {
                        selectedRoom.setIsOn(true);
                    }
                    hueController.toggleRoomOn(selectedRoom, !isOn);
                    RoomsActivity.updateRooms();
                    notifyDataSetChanged();
                }
            }else{
                if (getLayoutPosition() != 0) {
                    if (roomsToDelete.contains(selectedRoom)) {
                        roomsToDelete.remove(selectedRoom);
                        background.clearAnimation();
                        background.setBackgroundResource(R.drawable.room_item_off);
                    } else {
                        roomsToDelete.add(selectedRoom);
                        background.startAnimation(AnimationUtils.loadAnimation(RoomsActivity.mainContext, R.anim.shake));
                        background.setBackgroundResource(R.drawable.room_item_delete);
                    }
                }
            }
        }
    }
}
