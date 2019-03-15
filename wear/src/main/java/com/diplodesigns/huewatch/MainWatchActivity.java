package com.diplodesigns.huewatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.wearable.activity.ConfirmationActivity;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.WatchViewStub;
import android.support.wearable.view.WearableListView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;


public class MainWatchActivity extends WearableActivity{
    private String[] elements = { "Room 1", "Room 2", "Room 3", "Room 4", "Room 5", "Room 6"};
    private String TAG = "MainWatchActivity";
    private Context context;
    private Boolean firstTime = true;

    //Mobile connection variables
    private final String CONNECT_BRIDGE_PATH = "/connect-bridge";
    //private final String CONNECT_SUCCESS_PATH = "/bridge-success";
    private GoogleApiClient mGoogleApiClient;
    private String remoteNodeId;
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private static final long CONNECTION_TIME_OUT_MS = 100;

    //UI items
    private ProgressBar connectingBar;
    private WearableListView roomsView;
    private RoomsAdapter roomsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.wtf(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        context = this;
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.main_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                // Get the list component from the layout of the activity
                connectingBar = (ProgressBar) stub.findViewById(R.id.connectingBridgeBar);
                roomsView = (WearableListView) stub.findViewById(R.id.roomsListView);
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        // Now you can use the Data Layer API
                        Log.e(TAG, "WATCH onConnected: " + connectionHint);
                        requestBridgeAccess();
                    }
                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.e(TAG, "WATCH onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.e(TAG, "WATCH onConnectionFailed: " + result);
                    }
                })

                .addApi(Wearable.API)  // Request access only to the Wearable API
                .build();

        // Register the local broadcast receiver, defined in step 3.
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            roomsView.setBackgroundColor(getResources().getColor(android.R.color.black));

        } else {
            roomsView.setBackground(null);
        }
    }

    /**
     * Standard BroadcastReceiver called from ListenerService - with message as a JSON dictionary
     */
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.wtf(TAG, message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    connectingBar.setVisibility(View.INVISIBLE);
                }
            });

        }
    }

    private void requestBridgeAccess(){
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await(); //can use await(5, TimeUnit.SECONDS);
                for (Node node : nodes.getNodes()) {
                    if (node.isNearby()) { //ignore cloud - assumes one wearable attached
                        MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(
                                mGoogleApiClient, node.getId(), CONNECT_BRIDGE_PATH, "Connecting to bridge".getBytes()).await();
                        if (sendMessageResult.getStatus().isSuccess()) {
                            Log.e(TAG, "Message: { Connecting to bridge } sent to: " + node.getDisplayName());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                                    Log.wtf(TAG, "Bridge access requested");
                                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                                    intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Connecting to bridge");
                                }
                            });
                        }
                        else {
                            // Log an error
                            Log.e(TAG, "PHONE Failed to connect to Google Api Client with status "
                                    + sendMessageResult.getStatus());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                                    Log.wtf(TAG, "Bridge access requested");
                                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
                                    intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, "Failed to bridge");
                                }
                            });
                        }
                    }
                }
            }
        }).start();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!mResolvingError) {
            mGoogleApiClient.connect(); //connect to watch
        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();//disconnect from watch
        super.onStop();
    }

}
