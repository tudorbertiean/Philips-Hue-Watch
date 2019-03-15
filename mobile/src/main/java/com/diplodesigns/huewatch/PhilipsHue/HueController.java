package com.diplodesigns.huewatch.PhilipsHue;

import android.content.Context;
import android.util.Log;

import com.diplodesigns.huewatch.HueConnect.HueSharedPreferences;
import com.philips.lighting.hue.listener.PHHTTPListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHGroup;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Tudor on 3/29/2016.
 */
public class HueController {
    private PHBridge bridge;
    private Context context;
    private String GROUP_URL_ACTION;

    public HueController(PHBridge bridge, Context context){
        this.bridge = bridge;
        this.context = context;

        this.GROUP_URL_ACTION = "http://" + bridge.getResourceCache().getBridgeConfiguration().getIpAddress() + "/api/"+ HueSharedPreferences.getInstance(context).getUsername() + "/groups/";
    }

    public void toggleRoomOn(final PHGroup room, final Boolean isOn){
        String url = GROUP_URL_ACTION + room.getIdentifier() + "/action";
        String json;
        PHLightState state;
        //Parse the json and update the bridge's cache
        //with the new light states so the adapter can be
        //updated
        if (isOn) {
            json = "{\"on\": true, \"bri\": 254 }";
        }
        else {
            json = "{\"on\": false }";
        }

        for (String lightID : room.getLightIdentifiers()){
            for (PHLight light : bridge.getResourceCache().getAllLights()) {
                if (lightID.equals(light.getIdentifier())) {
                    state = light.getLastKnownLightState();
                    state.setOn(isOn);
                    if (isOn) {
                        state.setBrightness(254);
                    }
                    light.setLastKnownLightState(state);
                    break;
                }

            }
        }

        bridge.doHTTPPut(url, json, new PHHTTPListener() {
            @Override
            public void onHTTPResponse(String jsonResponse) {
                System.out.println("RESPONSE : " + jsonResponse);
            }
        });
    }

    public void dimRoom(final PHRoom room, final int dimness){
        String url = GROUP_URL_ACTION + room.getGroup().getIdentifier() + "/action";
        String json;
        if (dimness == 0){
            json = "{\"on\": false }";
        }else {
            json = "{\"on\": true, \"bri\": " + dimness + " }";
        }

        bridge.doHTTPPut(url, json, new PHHTTPListener() {
            @Override
            public void onHTTPResponse(String jsonResponse) {
                System.out.println("RESPONSE : " + jsonResponse);
                if (dimness == 0){

                }
            }
        });
    }

    public static void toggleRoomFromWatch(boolean isOn, String roomID, Context context) throws Exception{
        String json;

        if (isOn) {
            json = "{\"on\": true, \"bri\": 254 }";
        }
        else {
            json = "{\"on\": false }";

        }

        URL url = new URL("http://" + HueSharedPreferences.getInstance(context).getLastConnectedIPAddress() + "/api/" + HueSharedPreferences.getInstance(context).getUsername() + "/groups/" + roomID + "/action");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
        osw.write(json);
        osw.flush();
        osw.close();
        Log.wtf("HTTPPut", Integer.toString(connection.getResponseCode()));
    }

    public static void toggleLightFromWatch(boolean isOn, String lightID, Context context) throws Exception{
        String json;

        if (isOn) {
            json = "{\"on\": true, \"bri\": 254 }";
        }
        else {
            json = "{\"on\": false }";

        }

        URL url = new URL("http://" + HueSharedPreferences.getInstance(context).getLastConnectedIPAddress() + "/api/" + HueSharedPreferences.getInstance(context).getUsername() + "/lights/" + lightID + "/state");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
        osw.write(json);
        osw.flush();
        osw.close();
        Log.wtf("HTTPPut", Integer.toString(connection.getResponseCode()));
    }

    public static void dimRoomFromWatch(String message, Context context) throws Exception{
        List<String> messageList = Arrays.asList(message.split(","));
        String roomID = messageList.get(0);
        String progress = messageList.get(1);
        String json;

        if (Integer.valueOf(progress) == 0){
            json = "{\"on\": false }";
        }else {
            json = "{\"on\": true, \"bri\":" + progress + "}";
        }

        URL url = new URL("http://" + HueSharedPreferences.getInstance(context).getLastConnectedIPAddress() + "/api/" + HueSharedPreferences.getInstance(context).getUsername() + "/groups/" + roomID + "/action");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Accept", "application/json");
        OutputStreamWriter osw = new OutputStreamWriter(connection.getOutputStream());
        osw.write(json);
        osw.flush();
        osw.close();
        Log.wtf("HTTPPut", Integer.toString(connection.getResponseCode()));
    }
}
