package com.diplodesigns.huewatch;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
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
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.diplodesigns.huewatch.HueConnect.PHConnect;
import com.philips.lighting.hue.listener.PHGroupListener;
import com.philips.lighting.hue.sdk.PHAccessPoint;
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
    public static Context mainContext;
    public static Activity mainActivity;

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
    private static PHBridge mBridge;
    public Dialog pushLinkDialog;
    public boolean isConnected;
    private static TextView addGroupsTxt;

    private static Snackbar snackbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.add_room);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRoomDialog(new PHRoom(new PHGroup(), mBridge), true, true);
            }
        });

        mainContext = this;
        mainActivity = RoomsActivity.this;

        //Set listener to update activity when bridge connects or if it needs authentication
        if (!isConnected) {
            PHConnect.setConnectListener(hueListener);
            PHConnect.connectBridge(mainContext, false);
        }else{
            setupAdapter(mBridge);
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
    }

    @Override
    protected void onPause(){
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        initDeleteRooms = menu.findItem(R.id.initDeleteRoom);
        deleteRooms = menu.findItem(R.id.deleteSelected);

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
                initDeleteRooms.setVisible(false);
                roomsAdapter.deleteMode = true;
                roomsAdapter.notifyDataSetChanged();
                break;
            case R.id.deleteSelected:
                deleteRooms.setVisible(false);
                initDeleteRooms.setVisible(true);
                roomsAdapter.deleteMode = false;
                for (PHRoom roomToDelete : roomsAdapter.roomsToDelete){
                    mBridge.deleteGroup(roomToDelete.getGroup().getIdentifier(), groupListener);
                    roomsAdapter.notifyItemRemoved(allRooms.indexOf(roomToDelete));
                    allRooms.remove(roomToDelete);
                }
                roomsAdapter.notifyDataSetChanged();
                roomsAdapter.roomsToDelete.clear();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed(){
        //super.onBackPressed();
    }

    private void setupAdapter(final PHBridge bridge){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //If user is coming from authenticating, dismiss dialog
                if (pushLinkDialog != null && pushLinkDialog.isShowing())
                    pushLinkDialog.dismiss();

                allRooms = new ArrayList<>();
                addGroupsTxt = (TextView) findViewById(R.id.addGroupstext);
                refreshRooms = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_rooms);
                recyclerView = (RecyclerView) findViewById(R.id.rooms_recycle_view);
                hueController = new HueController(bridge, mainContext);

                refreshRooms.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
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
                for (PHLight light : bridge.getResourceCache().getAllLights()){
                    lightIDs.add(light.getIdentifier());
                }
                group0.setLightIdentifiers(lightIDs);
                allRooms.add(new PHRoom(group0, bridge));

                if (allRooms.size() == 0){
                    addGroupsTxt.setVisibility(View.VISIBLE);
                }else{
                    Collections.reverse(allRooms); //Reverse to show newest groups first
                }

                roomsAdapter = new RoomsAdapter(allRooms, hueController);

                GridLayoutManager mLayoutManager = new GridLayoutManager(getApplicationContext(), 2, GridLayoutManager.VERTICAL, false);
                recyclerView.setLayoutManager(mLayoutManager);
                recyclerView.setAdapter(roomsAdapter);
            }
        });
    }

    public static void updateRooms(){
        for (PHRoom room : allRooms){
            room.updateGroupState();
        }
    }

    /**
     *
     * @param room the room being displayed/created
     * @param isEdit determines if the lights should have switches or checkboxes,
     *               for either creating a new room or turning its lights on
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
                dismissText = (TextView) roomView.findViewById(R.id.dismissLayoutText);
                editRoomBtn = (ImageView) roomView.findViewById(R.id.editRoomBtn);
                dismissText.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        roomDialog.dismiss();
                        updateRooms();
                        roomsAdapter.notifyDataSetChanged();
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

    private PHConnect.HueListener hueListener = new PHConnect.HueListener() {
        @Override
        public void onError(int code, String message) {

        }

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
                    pushLinkDialog = new Dialog(mainContext);
                    pushLinkDialog.setContentView(R.layout.authenticate_layout);
                    pushLinkDialog.setTitle("Link to bridge");
                    pushLinkDialog.show();
                }
            });
        }
    };

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
                    if (roomDialog != null && roomDialog.isShowing()){
                        roomsAdapter.notifyDataSetChanged();
                        saveGroupProgress.setVisibility(View.INVISIBLE);
                        roomDialog.dismiss();
                        snackbar.make(mainActivity.getCurrentFocus(), "Group updated successfully", Snackbar.LENGTH_SHORT).show();
                    }else{
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

    public static void saveRoom(Boolean isNew) {
        if (roomName.getText().toString().equals("")) {
            Toast.makeText(mainContext, "Set a name first", Toast.LENGTH_SHORT).show();
        } else {
            if (isNew) {
                List<String> lightIDs = new ArrayList<>();
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
                List<String> lightIDs = new ArrayList<>();
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

    public void editRoom(View v){
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
        lightAdapter = new LightAdapter(mBridge, selectedRoom ,true, false);
        lightsRecycler.setAdapter(lightAdapter);
    }

    public static void setDismissDialog(){
        roomDim.setVisibility(View.VISIBLE);
        roomName.setEnabled(false);
        dismissText.setText("Dismiss");
        editRoomBtn.setVisibility(View.VISIBLE);
        lightAdapter = new LightAdapter(mBridge, selectedRoom, false, false);
        lightsRecycler.setAdapter(lightAdapter);
        dismissText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                roomDialog.dismiss();
                updateRooms();
                roomsAdapter.notifyDataSetChanged();
            }
        });
    }

}
