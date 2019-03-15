package com.diplodesigns.huewatch.Service;

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

    private final String GROUPS_PATH = "/bridge-groups";
    private final String LIGHTS_PATH = "/bridge-lights";
    private final String CONNECT_ERROR_PATH = "/bridge-error";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String message = new String(messageEvent.getData());
        Log.wtf(TAG, "WATCH Message path received on watch is: " + messageEvent.getPath());
        Log.wtf(TAG, "WATCH Message received on watch is: " + message);
        Intent messageIntent = new Intent();
        messageIntent.setAction(Intent.ACTION_SEND);
        messageIntent.putExtra("message", message);

        if (messageEvent.getPath().equals(GROUPS_PATH))  {
            messageIntent.putExtra("path", GROUPS_PATH);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        else if (messageEvent.getPath().equals(LIGHTS_PATH)){
            messageIntent.putExtra("path", LIGHTS_PATH);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        else if (messageEvent.getPath().equals(CONNECT_ERROR_PATH)){
            messageIntent.putExtra("path", CONNECT_ERROR_PATH);
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }
        else {
            messageIntent.putExtra("path", "unknown");
            LocalBroadcastManager.getInstance(this).sendBroadcast(messageIntent);
        }

        super.onMessageReceived(messageEvent);
    }
}
