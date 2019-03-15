package com.diplodesigns.huewatch;

import android.os.Bundle;
import android.util.Log;

import com.diplodesigns.huewatch.HueConnect.PHConnect;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.model.PHBridge;


/**
 * Created by Tudor on 3/31/2016.
 */
public class ListenerServiceMobile extends WearableListenerService{
    private final String CONNECT_BRIDGE_PATH = "/connect-bridge";
    private final String CONNECT_SUCCESS_PATH = "/bridge-success";
    private PHConnect.HueListener connectListener;
    private String TAG = "ListenerServiceMobile";
    private GoogleApiClient mGoogleApiClient;
    private String remoteNodeId;
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";
    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;

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
        mGoogleApiClient.disconnect(); //disconnect from watch
        super.onDestroy();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.wtf(TAG, "onMessageReceived");
        if (messageEvent.getPath().equals(CONNECT_BRIDGE_PATH)) {
            final String message = new String(messageEvent.getData());
            Log.wtf(TAG, "MOBILE Message path received on watch is: " + messageEvent.getPath());
            Log.wtf(TAG, "MOBILE Message received on watch is: " + message);
            PHConnect.setConnectListener(new PHConnect.HueListener() {
                @Override
                public void onBridgeConnected(PHBridge bridge) {
                    Log.wtf(TAG, "onBridgeConnect " + bridge.getResourceCache().getBridgeConfiguration().getBridgeID());
                    sendOnBridgeSuccess(bridge.getResourceCache().getBridgeConfiguration().getBridgeID());
                }

                @Override
                public void onAuthenticationRequired(PHAccessPoint accessPoint) {
                    Log.wtf(TAG, "onAuthentication");
                }

                @Override
                public void onError(int code, String message) {

                }
            });
            PHConnect.connectBridge(getApplicationContext(), true);
        }
        else {
            Log.wtf(TAG, "MOBILE Message path received FAILED path watch is: " + messageEvent.getPath());
            super.onMessageReceived(messageEvent);
        }
    }

    private void sendOnBridgeSuccess(final String bridgeID) {
        Log.wtf(TAG, "sendOnBridgeSuccess");
        new Thread( new Runnable() {
            @Override
            public void run() {
                        NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await(); //can use await(5, TimeUnit.SECONDS);
                        for (Node node : nodes.getNodes()) {
                            if(node.isNearby()) { //ignore cloud - assumes one wearable attached
                                MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(
                                        mGoogleApiClient, node.getId(), CONNECT_SUCCESS_PATH, bridgeID.getBytes()).await();
                                if (sendMessageResult.getStatus().isSuccess()) {
                                    Log.wtf(TAG, "Message: {" + bridgeID + "} sent to: " + node.getDisplayName());

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
