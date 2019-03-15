package com.diplodesigns.huewatch;

import android.content.Context;

import com.diplodesigns.huewatch.HueConnect.HueSharedPreferences;
import com.philips.lighting.hue.listener.PHHTTPListener;
import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHLight;
import com.philips.lighting.model.PHLightState;

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

    public void toggleRoomOn(final com.diplodesigns.huewatch.PHRoom room, final Boolean isOn){
        String url = GROUP_URL_ACTION + room.getGroup().getIdentifier() + "/action";
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

        for (String lightID : room.getGroup().getLightIdentifiers()){
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

    public void dimRoom(final com.diplodesigns.huewatch.PHRoom room, final int dimness){
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

    public PHBridge getBridge() {
        return bridge;
    }
}
