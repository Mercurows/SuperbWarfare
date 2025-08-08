package com.atsuishio.superbwarfare.data.mob_guns;

import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.google.gson.annotations.SerializedName;

public class DefaultMobGunData implements IDBasedData {
    @SerializedName("ID")
    String id = "";

    @Override
    public String getId() {
        return this.id;
    }

    @SerializedName("Probability")
    public double probability = 0;
    @SerializedName("GoalWeight")
    public int goalWeight = 3;
    @SerializedName("AimTime")
    public int aimTime = 20;
    @SerializedName("ClearAimTimeWhenLostSight")
    public boolean clearAimTimeWhenLostSight = true;

    ObjectToList<StringToObject<GunSpawnData>> guns;
}
