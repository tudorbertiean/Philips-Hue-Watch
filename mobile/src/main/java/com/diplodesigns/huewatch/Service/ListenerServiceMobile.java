package com.diplodesigns.huewatch.Service;

import android.os.Bundle;
import android.util.Log;

import com.diplodesigns.huewatch.HueConnect.PHConnect;
import com.diplodesigns.huewatch.PhilipsHue.HueController;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.model.PHBridge;


/**
 * Created by Tudor on 3/31/2016.
 */
public class ListenerServiceMobile extends WearableListenerService{
    private final String CONNECT_BRIDGE_PATH = "/connect-bridge";
    private final String LIGHTS_PATH = "/bridge-lights";
    private final String GROUPS_PATH = "/bridge-groups";
    private final String CONNECT_ERROR_PATH = "/bridge-error";
    private final String DISCONNECT_BRIDGE_PATH = "/disconnect-bridge";
    private static final String ROOM_ON_PATH = "room-on";
    private static final String ROOM_OFF_PATH = "room-off";
    private static final String ROOM_DIM_PATH = "room-dim";
    private static final String LIGHT_ON_PATH = "light-on";
    private static final String LIGHT_OFF_PATH = "light-off";

    private HueController controller;
    private String TAG = "ListenerServiceMobile";
    private GoogleApiClient mGoogleApiClient;
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.wtf(TAG, "onCreate");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        // Now you can use the Data Layer API
                        Log.wtf(TAG, "PHONE onConnected: " + connectionHint);
                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.wtf(TAG, "PHONE onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.wtf(TAG, "PHONE onConnectionFailed: " + result);
                    }
                })

                .addApi(Wearable.API)  // Request access only to the Wearable API
                .build();
        if (!mResolvingError) {
            mGoogleApiClient.connect(); //connect to watch
        }
    }

    @Override
    public void onDestroy() {
        Log.wtf(TAG, "onDestroy");
        mGoogleApiClient.disconnect(); //disconnect from watch
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.wtf(TAG, "onMessageReceived");
        final String message = new String(messageEvent.getData());
        Log.wtf(TAG, "MOBILE Message path received on watch is: " + messageEvent.getPath());
        Log.wtf(TAG, "MOBILE Message received on watch is: " + message);

        if (messageEvent.getPath().equals(CONNECT_BRIDGE_PATH)) {
            PHConnect.setConnectListener(new PHConnect.HueListener() {
                @Override
                public void onBridgeConnected(PHBridge bridge) {
                    Log.wtf(TAG, "onBridgeConnected " + bridge.getResourceCache().getBridgeConfiguration().getBridgeID());
                    Gson gson = new Gson();
                    sendOnBridgeMessage(gson.toJson(bridge.getResourceCache().getAllGroups()), GROUPS_PATH);
                    sendOnBridgeMessage(gson.toJson(bridge.getResourceCache().getAllLights()), LIGHTS_PATH);
                    controller = new HueController(bridge, getApplicationContext());
                }
                @Override
                public void onAuthenticationRequired(PHAccessPoint accessPoint) {
                    Log.wtf(TAG, "onAuthenticationRequired");
                    sendOnBridgeMessage("Authentication required", CONNECT_ERROR_PATH);
                }

                @Override
                public void onError(int code, String message) {
                    Log.wtf(TAG, "onAuthenticationRequired");
                    sendOnBridgeMessage("Error: " + message, CONNECT_ERROR_PATH);
                }
            });
            PHConnect.connectBridge(getApplicationContext(), true);
        }
        else if(messageEvent.getPath().equals(DISCONNECT_BRIDGE_PATH)){
            PHConnect.disconnectBridge();
        }
        else if(messageEvent.getPath().equals(ROOM_ON_PATH)){
            try {
                HueController.toggleRoomFromWatch(true, message, getApplicationContext());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if(messageEvent.getPath().equals(ROOM_OFF_PATH)){
            try {
                HueController.toggleRoomFromWatch(false, message, getApplicationContext());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if(messageEvent.getPath().equals(ROOM_DIM_PATH)){
            try {
                HueController.dimRoomFromWatch(message, getApplicationContext());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if(messageEvent.getPath().equals(LIGHT_ON_PATH)){
            try {
                HueController.toggleLightFromWatch(true, message, getApplicationContext());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else if(messageEvent.getPath().equals(LIGHT_OFF_PATH)){
            try {
                HueController.toggleLightFromWatch(false, message, getApplicationContext());
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        else {
            Log.wtf(TAG, "MOBILE Message path received FAILED path watch is: " + messageEvent.getPath());
            super.onMessageReceived(messageEvent);
        }
    }

    private void sendOnBridgeMessage(final String message, final String path) {
        Log.wtf(TAG, "sendOnBridgeSuccess");
        new Thread( new Runnable() {
            @Override
            public void run() {
                        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await(); //can use await(5, TimeUnit.SECONDS);
                        for (Node node : nodes.getNodes()) {
                            if(node.isNearby()) { //ignore cloud - assumes one wearable attached
                                MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(
                                        mGoogleApiClient, node.getId(), path, message.getBytes()).await();
                                if (sendMessageResult.getStatus().isSuccess()) {
                                    Log.wtf(TAG, "Message: {" + message + "} sent to: " + node.getDisplayName());

                                } else {
                                    // Log an error
                                    Log.wtf(TAG, "PHONE Failed to connect to Google Api Client with status "
                                            + sendMessageResult.getStatus());

                                }
                            }
                        }
                    }
        }).start();
    }

}
