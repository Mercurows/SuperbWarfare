package com.atsuishio.superbwarfare.data.mob_guns;

import com.atsuishio.superbwarfare.data.IDBasedData;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringToObject;
import com.google.gson.annotations.SerializedName;

public class DefaultMobGunData implements IDBasedData<DefaultMobGunData> {

    private transient String id = "";

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @SerializedName("Probability")
    public double probability = 0;
    @SerializedName("GoalWeight")
    public int goalWeight = 3;

    ObjectToList<StringToObject<GunSpawnData>> guns;
}
