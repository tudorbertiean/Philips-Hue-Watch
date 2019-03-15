package com.diplodesigns.huewatch.PhilipsHue;

import com.philips.lighting.model.PHBridge;
import com.philips.lighting.model.PHGroup;
import com.philips.lighting.model.PHLight;

import java.util.Map;

/**
 * Custom PHGroup class that is not associated with the
 * PHBridge or the hue SDK. We created this to hold some extra
 * variables and functions that are not supported by the original PHGroup
 * class.
 */
public class PHRoom {
    private PHGroup group;
    private PHBridge bridge;
    private Boolean isOn;
    private Boolean isReachable;

    public PHRoom(PHGroup group, PHBridge bridge){
        this.group = group;
        this.bridge = bridge;

        updateGroupState();
    }

    /**
     * Updates the group's light states to determine whether the
     * whole group is turned on/off or if all lights in it are reachable
     * or not. This information is important so that groups can be displayed
     * in the RoomsAdapter properly.
     */
    public void updateGroupState(){
        //Check if all lights in the group are on, at least one has to be off or not reachable for isOn to be false
        Map<String, PHLight> allLights = bridge.getResourceCache().getLights();
        if (group.getLightIdentifiers() == null){
            this.isOn = false;
            return;
        }

        this.isReachable = false;
        this.isOn = true;
        for (String lightID: this.group.getLightIdentifiers()){
            if (allLights.containsKey(lightID)) {
                if (allLights.get(lightID).getLastKnownLightState().isReachable()) {
                    //At least one light is reachable, therefore set the group as reachable
                    this.isReachable = true;
                }
                if (!allLights.get(lightID).getLastKnownLightState().isOn()) {
                    //At least one light is not on, therefore set the group as off
                    this.isOn = false;
                }

                if (this.isReachable && !this.isOn) //Both conditions have been changed, don't need to look anymore
                    break;
            }
        }
    }

    public PHGroup getGroup() {
        return group;
    }

    public void setGroup(PHGroup group) {
        this.group = group;
    }

    public Boolean getIsOn() {
        return isOn;
    }

    public void setIsOn(Boolean isOn) {
        this.isOn = isOn;
    }

    public Boolean getIsReachable() {
        return isReachable;
    }

    public void setIsReachable(Boolean isReachable) {
        this.isReachable = isReachable;
    }
}
