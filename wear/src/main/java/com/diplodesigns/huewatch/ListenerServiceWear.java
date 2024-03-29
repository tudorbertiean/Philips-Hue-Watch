package com.diplodesigns.huewatch;

import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by Tudor on 4/2/2016.
 */
public class ListenerServiceWear extends WearableListenerService {
    String TAG = "SendMessage";

    private final String CONNECT_SUCCESS_PATH = "/bridge-success";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(CONNECT_SUCCESS_PATH))  {
            final String message = new String(messageEvent.getData());
            Log.wtf(TAG, "WATCH Message path received on watch is: " + messageEvent.getPath());
            Log.wtf(TAG, "WATCH Message received on watch is: " + message);
            // Broadcast message to wearable activity for display
            Intent messageIntent = new Intent();
            messageIntent.setAction(Intent.ACTION_SEND);
            messageIntent.putExtra("message", message);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        else {
            super.onMessageReceived(messageEvent);
        }
    }
}
