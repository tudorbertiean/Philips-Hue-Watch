package com.diplodesigns.huewatch;

import android.content.Context;
import android.support.wearable.view.WearableListView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by Tudor on 3/31/2016.
 */
public final class RoomsAdapter extends WearableListView.Adapter {
    private String[] mDataset;
    private final Context mContext;
    private final LayoutInflater mInflater;

    // Provide a suitable constructor (depends on the kind of dataset)
    public RoomsAdapter(Context context, String[] dataset) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mDataset = dataset;
    }

    // Provide a reference to the type of views you're using
    public static class ItemViewHolder extends WearableListView.ViewHolder {
        private TextView textView;
        public ItemViewHolder(View itemView) {
            super(itemView);
            // find the text view within the custom item's layout
            textView = (TextView) itemView.findViewById(R.id.roomName);
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
    public void onBindViewHolder(WearableListView.ViewHolder holder,
                                 int position) {
        // retrieve the text view
        ItemViewHolder itemHolder = (ItemViewHolder) holder;
        TextView view = itemHolder.textView;
        // replace text contents
        view.setText(mDataset[position]);
        // replace list item's metadata
        holder.itemView.setTag(position);
    }

    // Return the size of your dataset
    // (invoked by the WearableListView's layout manager)
    @Override
    public int getItemCount() {
        return mDataset.length;
    }
}
