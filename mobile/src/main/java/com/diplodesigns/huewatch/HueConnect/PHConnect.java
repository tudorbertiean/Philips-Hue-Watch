package com.diplodesigns.huewatch.HueConnect;

import android.content.Context;
import android.util.Log;

import com.diplodesigns.huewatch.Main.RoomsActivity;
import com.philips.lighting.hue.sdk.PHAccessPoint;
import com.philips.lighting.hue.sdk.PHBridgeSearchManager;
import com.philips.lighting.hue.sdk.PHHueSDK;
import com.philips.lighting.hue.sdk.PHMessageType;
import com.philips.lighting.hue.sdk.PHSDKListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHHueParsingError;

import java.util.List;

/**
 * PHHomeActivity - The starting point in your own Hue App.
 *
 * For first time use, a Bridge search (UPNP) is performed and a list of all available bridges is displayed (and clicking one of them shows the PushLink dialog allowing authentication).
 * The last connected Bridge IP Address and Username are stored in SharedPreferences.
 *
 * For subsequent usage the app automatically connects to the last connected bridge.
 * When connected the MyApplicationActivity Activity is started.  This is where you should start implementing your Hue App!  Have fun!
 *
 * For explanation on key concepts visit: https://github.com/PhilipsHue/PhilipsHueSDK-Java-MultiPlatform-Android
 *
 *
 */
public class PHConnect{
    public static PHHueSDK phHueSDK;
    public static final String TAG = "PHConnect";
    private static com.diplodesigns.huewatch.HueConnect.HueSharedPreferences prefs;
    private static com.diplodesigns.huewatch.HueConnect.AccessPointListAdapter adapter;
    public static HueListener onBridgeConnectingListener;
    private static boolean lastSearchWasIPScan;
    private static Boolean fromWatch;


    //These methods create custom listeners for connecting/updating ui to make it dynamic
    public interface HueListener{
        void onBridgeConnected(PHBridge bridge); //Called when user connects to the bridge
        void onAuthenticationRequired(PHAccessPoint accessPoint); //Need to authenticate to link to bridge
        void onError(int code, String message); //Error connecting to bridge, message contains details
    }

    public static void setConnectListener(HueListener listener){
        PHConnect.onBridgeConnectingListener = listener;
    }

    public static void connectBridge(Context context, Boolean watch){
        fromWatch = watch;
        phHueSDK = PHHueSDK.create();
        lastSearchWasIPScan = false;
        Log.wtf(TAG, "Connecting");
        // Set the Device Name (name of your app). This will be stored in your bridge whitelist entry.
        phHueSDK.setAppName("HueWatch");
        phHueSDK.setDeviceName(android.os.Build.MODEL);

        // Register the PHSDKListener to receive callbacks from the bridge.
        try{
            phHueSDK.getNotificationManager().unregisterSDKListener(listener);
            phHueSDK.getNotificationManager().registerSDKListener(listener);
        }catch (Exception e){
            phHueSDK.getNotificationManager().registerSDKListener(listener);
        }

        adapter = new AccessPointListAdapter(context, phHueSDK.getAccessPointsFound());

        //ListView accessPointList = (ListView) findViewById(R.id.bridge_list);
//        accessPointList.setOnItemClickListener(this);
//        accessPointList.setAdapter(adapter);

        // Try to automatically connect to the last known bridge.  For first time use this will be empty so a bridge search is automatically started.
        prefs = HueSharedPreferences.getInstance(context);
        String lastIpAddress = prefs.getLastConnectedIPAddress();
        String lastUsername = prefs.getUsername();

        // Automatically try to connect to the last connected IP Address.  For multiple bridge support a different implementation is required.
        if (lastIpAddress !=null && !lastIpAddress.equals("")) {
            PHAccessPoint lastAccessPoint = new PHAccessPoint();
            lastAccessPoint.setIpAddress(lastIpAddress);
            lastAccessPoint.setUsername(lastUsername);
            Log.wtf(TAG, "Getting info from shared preferences, checking for access");
            if (!phHueSDK.isAccessPointConnected(lastAccessPoint)) {
                Log.wtf(TAG, "No prev access connected, connecting to prev bridge");
                phHueSDK.connect(lastAccessPoint);
            }else{
                Log.wtf(TAG, "Already connected");
                phHueSDK.disableAllHeartbeat();
                phHueSDK.disconnect(phHueSDK.getSelectedBridge());
                try{
                    phHueSDK.connect(lastAccessPoint);
                }catch (Exception e){
                    onBridgeConnectingListener.onBridgeConnected(phHueSDK.getSelectedBridge());
                }
            }
        }
        else {  // First time use, so perform a bridge search.
            Log.wtf(TAG, "First time use, searching for bridge on network");
            doBridgeSearch();
        }
    }

    // Local SDK Listener
    private static PHSDKListener listener = new PHSDKListener() {
        @Override
        public void onAccessPointsFound(List<PHAccessPoint> accessPoint) {
            Log.wtf(TAG, "Access Points Found. " + accessPoint.size());
            if (accessPoint != null && accessPoint.size() > 0) {
                phHueSDK.getAccessPointsFound().clear();
                phHueSDK.getAccessPointsFound().addAll(accessPoint);
                phHueSDK.connect(accessPoint.get(0));
            }
        }

        @Override
        public void onCacheUpdated(List<Integer> arg0, PHBridge bridge) {
            Log.w(TAG, "On CacheUpdated");

        }

        @Override
        public void onBridgeConnected(PHBridge b, String username) {
            Log.wtf(TAG, "onBridgeConnected");
            phHueSDK.disableAllHeartbeat();
            phHueSDK.setSelectedBridge(b);
            if (!fromWatch) {
                phHueSDK.enableHeartbeat(b, PHHueSDK.HB_INTERVAL);
                phHueSDK.getLastHeartbeat().put(b.getResourceCache().getBridgeConfiguration().getIpAddress(), System.currentTimeMillis());
            }else{
                phHueSDK.getNotificationManager().unregisterSDKListener(listener);
            }
            prefs.setLastConnectedIPAddress(b.getResourceCache().getBridgeConfiguration().getIpAddress());
            prefs.setUsername(username);

            onBridgeConnectingListener.onBridgeConnected(b);
        }

        @Override
        public void onAuthenticationRequired(PHAccessPoint accessPoint) {
            Log.w(TAG, "Authentication Required.");
            phHueSDK.startPushlinkAuthentication(accessPoint);

            onBridgeConnectingListener.onAuthenticationRequired(accessPoint);
        }

        @Override
        public void onConnectionResumed(PHBridge bridge) {
            Log.w(TAG, "onConnectionResumed.");
            if(!fromWatch) {
                if (RoomsActivity.mainActivity.isFinishing())
                    return;
            }

            Log.v(TAG, "onConnectionResumed" + bridge.getResourceCache().getBridgeConfiguration().getIpAddress());
            phHueSDK.getLastHeartbeat().put(bridge.getResourceCache().getBridgeConfiguration().getIpAddress(),  System.currentTimeMillis());
            for (int i = 0; i < phHueSDK.getDisconnectedAccessPoint().size(); i++) {

                if (phHueSDK.getDisconnectedAccessPoint().get(i).getIpAddress().equals(bridge.getResourceCache().getBridgeConfiguration().getIpAddress())) {
                    phHueSDK.getDisconnectedAccessPoint().remove(i);
                }
            }

        }

        @Override
        public void onConnectionLost(PHAccessPoint accessPoint) {
            Log.v(TAG, "onConnectionLost : " + accessPoint.getIpAddress());
            if (!phHueSDK.getDisconnectedAccessPoint().contains(accessPoint)) {
                phHueSDK.getDisconnectedAccessPoint().add(accessPoint);
            }
        }

        @Override
        public void onError(int code, final String message) {
            Log.e(TAG, "on Error Called : " + code + ":" + message);
            if (code == PHMessageType.BRIDGE_NOT_FOUND) {
                if (!lastSearchWasIPScan) {  // Perform an IP Scan (backup mechanism) if UPNP and Portal Search fails.
                    phHueSDK = PHHueSDK.getInstance();
                    PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
                    sm.search(false, false, true);
                    lastSearchWasIPScan=true;
                    return;
                }
            }

            onBridgeConnectingListener.onError(code, message);
        }

        @Override
        public void onParsingErrors(List<PHHueParsingError> parsingErrorsList) {
            for (PHHueParsingError parsingError: parsingErrorsList) {
                Log.e(TAG, "ParsingError : " + parsingError.getMessage());
            }
        }
    };

    public static void doBridgeSearch() {
        PHBridgeSearchManager sm = (PHBridgeSearchManager) phHueSDK.getSDKService(PHHueSDK.SEARCH_BRIDGE);
        // Start the UPNP Searching of local bridges.
        sm.search(true, true);
    }

    public static void disconnectBridge(){
        Log.wtf(TAG, "Disconnecting bridge");
        try{
            phHueSDK = PHHueSDK.getInstance();
        }catch (Exception e){
            phHueSDK = PHHueSDK.create();
        }

        try{
            phHueSDK.getNotificationManager().unregisterSDKListener(listener);
            phHueSDK.disableAllHeartbeat();
            phHueSDK.disconnect(phHueSDK.getSelectedBridge());
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
