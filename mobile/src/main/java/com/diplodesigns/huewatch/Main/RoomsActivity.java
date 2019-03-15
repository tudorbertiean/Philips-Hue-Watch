package com.diplodesigns.huewatch.Main;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.diplodesigns.huewatch.HueConnect.PHConnect;
import com.diplodesigns.huewatch.PhilipsHue.HueController;
import com.diplodesigns.huewatch.PhilipsHue.PHRoom;
import com.diplodesigns.huewatch.R;
import com.philips.lighting.hue.listener.PHGroupListener;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHBridgeResource;
import com.philips.lighting.model.PHGroup;
import com.philips.lighting.model.PHHueError;
import com.philips.lighting.model.PHLight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RoomsActivity extends AppCompatActivity {
    //Adapter items
    private static RoomsAdapter roomsAdapter;
    private RecyclerView recyclerView;
    private static List<PHRoom> allRooms;
    private SwipeRefreshLayout refreshRooms;

    //Reference activity
    private MenuItem initDeleteRooms;
    private MenuItem deleteRooms;
    private static MenuItem infoBtn;
    public static Context mainContext;
    public static Activity mainActivity;
    private static String TAG = "RoomsActivity";

    //Room settings views
    public static TextView dismissText;
    public static TextView roomName;
    public static SeekBar roomDim;
    private static RecyclerView lightsRecycler;
    private static LightAdapter lightAdapter;
    public static Dialog roomDialog;
    private static View roomView;
    private static ImageView editRoomBtn;
    private static ProgressBar saveGroupProgress;
    private static PHRoom selectedRoom;
    private static HueController hueController;

    //Connecting to bridge variables
    private PHHueSDK phSDK;
    private static PHBridge mBridge;
    public Dialog pushLinkDialog;
    public boolean isConnected;
    private static TextView addGroupsTxt;

    private ProgressBar pushlinkBar;
    private TextView connectErrorTxt;
    private ImageView refreshImg;
    private static Snackbar snackbar;
    private static FloatingActionButton fab;
    private static Animation refreshAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connectErrorTxt = (TextView) findViewById(R.id.connectErrorTxt);
        refreshImg = (ImageView) findViewById(R.id.refreshConnectImg);
        refreshAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_refresh);
        refreshImg.startAnimation(refreshAnimation);

        fab = (FloatingActionButton) findViewById(R.id.add_room);
        fab.setVisibility(View.INVISIBLE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRoomDialog(new PHRoom(new PHGroup(), mBridge), true, true);
            }
        });

        mainContext = this;
        mainActivity = RoomsActivity.this;
        phSDK = PHHueSDK.create();
        //Set listener to update activity when bridge connects or if it needs authentication
        if (!isConnected) {
            PHConnect.setConnectListener(hueListener);
            PHConnect.connectBridge(mainContext, false);
        }else{
            setupAdapter(mBridge);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        initDeleteRooms = menu.findItem(R.id.initDeleteRoom);
        deleteRooms = menu.findItem(R.id.deleteSelected);
        infoBtn = menu.findItem(R.id.infoBtn);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        switch(id){
            case R.id.initDeleteRoom:
                deleteRooms.setVisible(true);
                fab.setVisibility(View.INVISIBLE);
                initDeleteRooms.setVisible(false);
                roomsAdapter.deleteMode = true;
                roomsAdapter.notifyDataSetChanged();
                break;
            case R.id.deleteSelected:
                deleteRooms.setVisible(false);
                initDeleteRooms.setVisible(true);
                fab.setVisibility(View.VISIBLE);
                roomsAdapter.deleteMode = false;
                for (PHRoom roomToDelete : roomsAdapter.roomsToDelete){
                    mBridge.deleteGroup(roomToDelete.getGroup().getIdentifier(), groupListener);
                    roomsAdapter.notifyItemRemoved(allRooms.indexOf(roomToDelete));
                    allRooms.remove(roomToDelete);
                }
                roomsAdapter.notifyDataSetChanged();
                roomsAdapter.roomsToDelete.clear();
                break;
            case R.id.infoBtn:
                final AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
                builder.setTitle("Info");
                LayoutInflater inflater = mainActivity.getLayoutInflater();
                builder.setView(inflater.inflate(R.layout.info_dialog, null));
                builder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroy(){
        try {
            //App is closed, disconnect all Philips components
            phSDK.disconnect(mBridge);
            phSDK.disableAllHeartbeat();
        }catch (Exception e){

        }
        super.onDestroy();
    }

    /**
     * The main adapter that runs on the main screen, it contains
     * the rooms with the # of lights and their names.
     * This is called when user connects to the bridge
     *
     * @param bridge containing the connected bridge
     */
    private void setupAdapter(final PHBridge bridge){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Set up activity for connect success
                connectErrorTxt.setVisibility(View.INVISIBLE);
                refreshImg.setVisibility(View.INVISIBLE);
                if (pushLinkDialog != null && pushLinkDialog.isShowing())
                    pushLinkDialog.dismiss();
                initDeleteRooms.setVisible(true);
                infoBtn.setVisible(true);
                fab.setVisibility(View.VISIBLE);
                refreshImg.clearAnimation();

                //Start getting data for rooms to display
                allRooms = new ArrayList<>();
                addGroupsTxt = (TextView) findViewById(R.id.addGroupstext);
                refreshRooms = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_rooms);
                refreshRooms.setVisibility(View.VISIBLE);
                recyclerView = (RecyclerView) findViewById(R.id.rooms_recycle_view);
                hueController = new HueController(bridge, mainContext);

                refreshRooms.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        updateRooms();
                        roomsAdapter.notifyDataSetChanged();
                        refreshRooms.setRefreshing(false);
                    }
                });

                //Using the groups, create our custom objects and add to adapter
                for (PHGroup group : bridge.getResourceCache().getAllGroups()) {
                    PHRoom room = new PHRoom(group, bridge);
                    allRooms.add(room);
                }

                //Create default group of all lights
                PHGroup group0 = new PHGroup();
                group0.setIdentifier("0");
                group0.setName("All lights");
                List<String> lightIDs = new ArrayList<>();
                for (PHLight light : bridge.getResourceCache().getAllLights()) {
                    lightIDs.add(light.getIdentifier());
                }
                group0.setLightIdentifiers(lightIDs);
                allRooms.add(new PHRoom(group0, bridge));

                if (allRooms.size() == 0) {
                    addGroupsTxt.setVisibility(View.VISIBLE);
                } else {
                    Collections.reverse(allRooms); //Reverse to show newest groups first
                }

                roomsAdapter = new RoomsAdapter(allRooms, hueController);

                GridLayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 2, GridLayoutManager.VERTICAL, false);
                recyclerView.setLayoutManager(mLayoutManager);
                recyclerView.setAdapter(roomsAdapter);

            }
        });
    }

    /**
     * Checks for updates in the room states, like if lights
     * go unreachable, or are turned on/off.
     * This is called on the swipedown refresh
     */
    public static void updateRooms(){
        for (PHRoom room : allRooms){
            room.updateGroupState();
        }
    }

    /**
     * Creates the dialog to show the user the room settings
     *
     * @param room the room being displayed/created
     * @param isEdit determines if the lights should have switches or checkboxes,
     *               for either creating a new room or turning its lights on
     * @param isNew determines if the roomName should be enabled to edit or not,
     *              editible if isNew or user wants to edit
     */
    public static void showRoomDialog(final PHRoom room, final Boolean isEdit, final Boolean isNew){
        // Custom room dialog
        mainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                selectedRoom = room;
                roomDialog = new Dialog(mainContext, R.style.CustomDialog);
                roomView = mainActivity.getLayoutInflater().inflate(R.layout.room_settings_layout, null);
                roomDialog.setContentView(roomView);
                roomDialog.setCanceledOnTouchOutside(true);
                roomDialog.show();

                roomDim = (SeekBar) roomView.findViewById(R.id.groupDim);
                try{ //Will fail if user clicked on add new room, because no room made yet
                    if (!selectedRoom.getIsReachable())
                        roomDim.setEnabled(false);
                }catch (Exception e){

                }
                dismissText = (TextView) roomView.findViewById(R.id.dismissLayoutText);
                editRoomBtn = (ImageView) roomView.findViewById(R.id.editRoomBtn);
                dismissText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        roomDialog.dismiss();
                    }
                });

                roomName = (AutoCompleteTextView) roomView.findViewById(R.id.roomNameText);
                if(!isNew) {
                    roomName.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            if (!s.toString().equals(room.getGroup().getName())) {
                                dismissText.setText("Save");
                                dismissText.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        saveRoom(false);
                                    }
                                });
                            }
                        }
                    });
                    roomName.setEnabled(false);
                    roomName.setText(room.getGroup().getName());
                    editRoomBtn.setVisibility(View.VISIBLE);
                }
                else {
                    roomDim.setVisibility(View.GONE);
                    dismissText.setText("Cancel");
                    roomName.setHint("Room name");
                    editRoomBtn.setVisibility(View.INVISIBLE);
                }

                saveGroupProgress = (ProgressBar) roomView.findViewById(R.id.saveGroupProgress);
                lightsRecycler = (RecyclerView) roomView.findViewById(R.id.lights_recycle_view);
                lightAdapter = new LightAdapter(mBridge, room, isEdit, isNew);
                LinearLayoutManager mLayoutManager = new LinearLayoutManager(mainContext);
                lightsRecycler.addItemDecoration(new DividerItemDecoration(mainActivity.getResources().getDrawable(R.drawable.light_divider)));
                lightsRecycler.setLayoutManager(mLayoutManager);
                lightsRecycler.setAdapter(lightAdapter);

                roomDim.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        hueController.dimRoom(room, seekBar.getProgress());
                    }
                });
            }
        });
    }

    /**
     * Creates or updates a room on the bridge when save button is clicked on room dialog
     * @param isNew - determines whether to update room, or create a new one
     */
    public static void saveRoom(Boolean isNew) {
        if (roomName.getText().toString().equals("")) { //Don't allow if no name is set
            Toast.makeText(mainContext, "Set a name first", Toast.LENGTH_SHORT).show();
        } else {
            List<String> lightIDs = new ArrayList<>();
            if (isNew) {
                PHGroup group = new PHGroup();
                group.setName(RoomsActivity.roomName.getText().toString());
                for (PHLight light : LightAdapter.roomLights) {
                    lightIDs.add(light.getIdentifier());
                }
                group.setLightIdentifiers(lightIDs);
                mBridge.createGroup(group, groupListener);
                saveGroupProgress.setVisibility(View.VISIBLE);
                dismissText.setVisibility(View.INVISIBLE);
            }
            else{
                PHGroup group = selectedRoom.getGroup();
                group.setName(RoomsActivity.roomName.getText().toString());
                for (PHLight light : LightAdapter.roomLights) {
                    lightIDs.add(light.getIdentifier());
                }
                group.setLightIdentifiers(lightIDs);
                mBridge.updateGroup(group, groupListener);
                saveGroupProgress.setVisibility(View.VISIBLE);
                dismissText.setVisibility(View.INVISIBLE);
            }
        }
    }

    /**
     * Changes the roomDialog to edit mode,
     * when user is making changes to a room
     */
    public void setEditDialog(View v){
        roomDim.setVisibility(View.GONE);
        editRoomBtn.setVisibility(View.INVISIBLE);
        roomName.setEnabled(true);
        dismissText.setText("Cancel");
        dismissText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setDismissDialog();
            }
        });
        lightAdapter = new LightAdapter(mBridge, selectedRoom ,true, false); // true for isEdit, false for isNew
        lightsRecycler.setAdapter(lightAdapter);
    }

    /**
     * Changes the roomDialog to dismiss mode,
     * when no changes have been made
     */
    public static void setDismissDialog(){
        roomDim.setVisibility(View.VISIBLE);
        roomName.setEnabled(false);
        dismissText.setText("Dismiss");
        editRoomBtn.setVisibility(View.VISIBLE);
        lightAdapter = new LightAdapter(mBridge, selectedRoom, false, false); // false for isEdit, false for isNew
        lightsRecycler.setAdapter(lightAdapter);
        dismissText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomDialog.dismiss();
            }
        });
    }

    /**
     * Called when the refresh imageView is called,
     * tried to connect user to their bridge
     * @param view
     */
    public void connectToBridge(View view) {
        refreshImg.startAnimation(refreshAnimation);
        PHConnect.connectBridge(mainContext, false);
    }

    /**
     * Listener for connecting events, created a custom
     * listener so that each UI can be updated individually
     */
    private PHConnect.HueListener hueListener = new PHConnect.HueListener() {
        @Override
        public void onBridgeConnected(PHBridge bridge) {
            isConnected = true;
            mBridge = bridge;
            setupAdapter(bridge);
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Custom dialog
                    pushLinkDialog = new Dialog(mainContext, R.style.PushlinkDialog);
                    roomView = mainActivity.getLayoutInflater().inflate(R.layout.pushlink_layout, null);
                    pushlinkBar = (ProgressBar) roomView.findViewById(R.id.pushlinkBar);
                    pushLinkDialog.setContentView(roomView);
                    pushLinkDialog.setCancelable(false);
                    pushLinkDialog.show();
                }
            });
        }

        @Override
        public void onError(final int code, String message) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (code == PHMessageType.PUSHLINK_BUTTON_NOT_PRESSED) {
                        pushlinkBar.incrementProgressBy(1);
                        return;
                    }
                    else if (code == PHHueError.NO_CONNECTION){
                        return;
                    }
                    else if (code == PHHueError.AUTHENTICATION_FAILED || code == PHMessageType.PUSHLINK_AUTHENTICATION_FAILED) {
                        pushLinkDialog.dismiss();
                    }
                    else if (code == PHHueError.BRIDGE_NOT_RESPONDING){

                    }else if (code == PHMessageType.BRIDGE_NOT_FOUND){

                    }else{
                        return;
                    }
                    connectErrorTxt.setVisibility(View.VISIBLE);
                    refreshImg.clearAnimation();
                    refreshImg.setVisibility(View.VISIBLE);
                    try{
                        recyclerView.setVisibility(View.INVISIBLE);
                    }catch (Exception e){

                    }
                }
            });
        }
    };

    /**
     * Listener for creating/updating/deleting rooms on bridge
     */
    private static PHGroupListener groupListener = new PHGroupListener() {
        @Override
        public void onCreated(final PHGroup phGroup) {
            Log.d("Grouplistener", "onCreated");
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addGroupsTxt.setVisibility(View.INVISIBLE);
                    PHRoom room = new PHRoom(phGroup, mBridge);
                    allRooms.add(1, room);
                    roomsAdapter.notifyItemInserted(1);
                    roomDialog.dismiss();
                    snackbar.make(mainActivity.getCurrentFocus(), "Group created successfully", Snackbar.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onReceivingGroupDetails(PHGroup phGroup) {
            Log.d("Grouplistener", "onReceivingGroupDetails: " + phGroup.getName());
        }

        @Override
        public void onReceivingAllGroups(List<PHBridgeResource> list) {
            Log.d("Grouplistener", "onReceivingAllGroups");
        }

        @Override
        public void onSuccess() {
            Log.wtf("Grouplistener", "onSuccess");
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (roomDialog != null && roomDialog.isShowing()){ //onSuccess b/c group updated
                        roomsAdapter.notifyDataSetChanged();
                        saveGroupProgress.setVisibility(View.INVISIBLE);
                        roomDialog.dismiss();
                        snackbar.make(mainActivity.getCurrentFocus(), "Group updated successfully", Snackbar.LENGTH_SHORT).show();
                    }
                    else{ //onSuccess b/c group deleted
                        snackbar.make(mainActivity.getCurrentFocus(), "Group(s) deleted successfully", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @Override
        public void onError(int i, String s) {
            Log.d("Grouplistener", "onError: " + s);
            mainActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    roomDialog.dismiss();
                    snackbar.make(mainActivity.getCurrentFocus(), "There has been an error, try again", Snackbar.LENGTH_LONG).show();
                }
            });
        }

        @Override
        public void onStateUpdate(Map<String, String> map, List<PHHueError> list) {
            Log.d("Grouplistener", "onStateUpdate");

        }
    };
}
