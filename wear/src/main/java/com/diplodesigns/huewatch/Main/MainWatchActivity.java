package com.diplodesigns.huewatch.Main;

import android.app.Activity;
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
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.diplodesigns.huewatch.R;
import com.diplodesigns.huewatch.RoomSettings.RoomSettingsActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.philips.lighting.model.PHGroup;
import com.philips.lighting.model.PHLight;

import java.lang.reflect.Type;
import java.util.List;

public class MainWatchActivity extends WearableActivity{
    private String[] elements = { "Room 1", "Room 2", "Room 3", "Room 4", "Room 5", "Room 6"};
    private static String TAG = "MainWatchActivity";
    private static Context context;
    private static Activity activity;
    private Boolean firstTime = true;

    //Mobile connection variables
    private final String CONNECT_BRIDGE_PATH = "/connect-bridge";
    private final String CONNECT_ERROR_PATH = "/bridge-error";
    private final String GROUPS_PATH = "/bridge-groups";
    private final String LIGHTS_PATH = "/bridge-lights";
    private final String DISCONNECT_BRIDGE_PATH = "/disconnect-bridge";
    private static GoogleApiClient mGoogleApiClient;
    // Bool to track whether the app is already resolving an error
    private boolean mResolvingError = false;

    //UI items
    private ProgressBar connectingBar;
    private WearableListView roomsView;
    private RoomsAdapter roomsAdapter;
    private RelativeLayout watchBackground;
    private List<PHGroup> rooms;
    public static List<PHLight> lights;
    private TextView errConnecting;
    private ImageView refreshImg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.wtf(TAG, "onCreate");
        setContentView(R.layout.activity_main);
        setAmbientEnabled();
        context = this;
        activity = this;
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.main_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                // Get the list component from the layout of the activity
                connectingBar = (ProgressBar) stub.findViewById(R.id.connectingBridgeBar);
                roomsView = (WearableListView) stub.findViewById(R.id.roomsListView);
                watchBackground = (RelativeLayout) stub.findViewById(R.id.mainBackground);
                errConnecting = (TextView) stub.findViewById(R.id.errConnectTxt);
                refreshImg = (ImageView) stub.findViewById(R.id.refreshImg);
                refreshImg.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendMessage("Connecting to bridge", CONNECT_BRIDGE_PATH);
                        refreshImg.setVisibility(View.INVISIBLE);
                        connectingBar.setVisibility(View.VISIBLE);
                        errConnecting.setVisibility(View.INVISIBLE);
                    }
                });
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        // Now you can use the Data Layer API
                        Log.e(TAG, "WATCH onConnected: " + connectionHint);
                        sendMessage("Connecting to bridge", CONNECT_BRIDGE_PATH);
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

        // Register the local broadcast receiver
        IntentFilter messageFilter = new IntentFilter(Intent.ACTION_SEND);
        MessageReceiver messageReceiver = new MessageReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, messageFilter);
    }

    public static void startRoomActivity(PHGroup room){
        Intent intent = new Intent(context, RoomSettingsActivity.class);
        Gson gson = new Gson();
        intent.putExtra("Room", gson.toJson(room));
        context.startActivity(intent);
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
         //   watchBackground.setBackgroundColor(getResources().getColor(R.color.ambientBackground));
        } else {
           // watchBackground.setBackground(null);
        }
    }

    /**
     * Standard BroadcastReceiver called from ListenerService - with message as a JSON dictionary
     */
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, Intent intent) {
            final String path = intent.getStringExtra("path");
            final String message = intent.getStringExtra("message");
            Log.wtf(TAG, path);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Gson gson = new Gson();
                    if (path.equals(GROUPS_PATH)) {
                        Type type = new TypeToken<List<PHGroup>>() {
                        }.getType();
                        rooms = gson.fromJson(message, type);
                        connectingBar.setVisibility(View.INVISIBLE);
                        roomsAdapter = new RoomsAdapter(context, rooms);
                        roomsView.setAdapter(roomsAdapter);
                        try {
                            roomsView.scrollToPosition(1);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                    } else if (path.equals(LIGHTS_PATH)) {
                        Type type = new TypeToken<List<PHLight>>() {
                        }.getType();
                        lights = gson.fromJson(message, type);
                        connectingBar.setVisibility(View.INVISIBLE);
                    } else if (path.equals(CONNECT_ERROR_PATH)) {
                        connectingBar.setVisibility(View.INVISIBLE);
                        errConnecting.setVisibility(View.VISIBLE);
                        refreshImg.setVisibility(View.VISIBLE);
                    } else {
                        errConnecting.setVisibility(View.VISIBLE);
                        connectingBar.setVisibility(View.INVISIBLE);
                        refreshImg.setVisibility(View.VISIBLE);
                    }
                }
            });
        }
    }

    public static void sendMessage(final String message, final String path){
        new Thread( new Runnable() {
            @Override
            public void run() {
                NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await(); //can use await(5, TimeUnit.SECONDS);
                for (Node node : nodes.getNodes()) {
                    if (node.isNearby()) { //ignore cloud - assumes one wearable attached
                        MessageApi.SendMessageResult sendMessageResult = Wearable.MessageApi.sendMessage(
                                mGoogleApiClient, node.getId(), path, message.getBytes()).await();
                        if (sendMessageResult.getStatus().isSuccess()) {
                            Log.wtf(TAG, "Message: {" + message + "} sent to: " + node.getDisplayName());
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(context, ConfirmationActivity.class);
                                    Log.wtf(TAG, "Message sent successfully");
                                    intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                                    intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, message);
                                }
                            });
                        }
                        else {
                            // Log an error
                            Log.e(TAG, "PHONE Failed to connect to Google Api Client with status "
                                    + sendMessageResult.getStatus());
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Intent intent = new Intent(context, ConfirmationActivity.class);
                                    Log.wtf(TAG, "Message did not send successfully");
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
    protected void onDestroy() {
        Log.wtf(TAG, "onDestroy");
        mGoogleApiClient.disconnect(); //disconnect from watch
        super.onDestroy();
    }
}
