package com.atsuishio.superbwarfare.data.mob_guns;

import com.atsuishio.superbwarfare.data.DeserializeFromString;
import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

public class GunSpawnData implements DeserializeFromString {
    @SerializedName("ID")
    String id = "";

    @SerializedName("Weight")
    public int weight = 1;

    @SerializedName("AimTime")
    public int aimTime = 30;
    @SerializedName("ClearAimTimeWhenLostSight")
    public boolean clearAimTimeWhenLostSight = true;
    @SerializedName("SemiFireInterval")
    public long semiFireInterval = 500;

    @SerializedName("BackupAmmo")
    public int backupAmmo;
    @SerializedName("SpawnWithLoadedAmmo")
    public boolean spawnWithLoadedAmmo = true;

    @SerializedName("ShootDistance")
    public double shootDistance = 30;

    @SerializedName("Zoom")
    public boolean zoom = true;
    @SerializedName("Spread")
    public double spread = 1;

    // NBT data
    @SerializedName("Data")
    public JsonObject data;

    // property override
    @SerializedName("Override")
    public JsonObject override;

    @Override
    public void deserializeFromString(String str) {
        this.id = str;
    }
}
