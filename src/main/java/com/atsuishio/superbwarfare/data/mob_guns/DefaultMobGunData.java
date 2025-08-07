package com.atsuishio.superbwarfare.data.mob_guns;

import com.atsuishio.superbwarfare.data.IDBasedData;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class DefaultMobGunData implements IDBasedData {
    @SerializedName("ID")
    String id = "";

    public String gunID = "";

    @Override
    public String getId() {
        return this.id;
    }

    public double probability = 0;
    public int goalWeight = 3;

    public int backupAmmo;
    public boolean spawnWithLoadedAmmo = true;

    public double shootDistance = 30;


    // NBT data
    public JsonObject data;

    // property override
    public JsonObject override;
}
