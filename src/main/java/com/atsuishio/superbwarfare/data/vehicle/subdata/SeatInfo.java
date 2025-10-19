package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.atsuishio.superbwarfare.data.gun.DefaultGunData;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

public class SeatInfo {
    @SerializedName("HidePassenger")
    public boolean hidePassenger = false;

    @SerializedName("IsEnclosed")
    @ServerOnly
    public Boolean isEnclosed = null;

    // TODO 座位锚点
    @SerializedName("Transform")
    public String transform = "Default";

    @SerializedName("Position")
    public Vec3 position = Vec3.ZERO;

    @SerializedName("Orientation")
    public StringOrVec3 orientation = new StringOrVec3("Default");

    @SerializedName("MinPitch")
    public float minPitch = -20;

    @SerializedName("MaxPitch")
    public float maxPitch = 20;

    @SerializedName("MinYaw")
    public float minYaw = -90;

    @SerializedName("MaxYaw")
    public float maxYaw = 90;

    @SerializedName("WeaponData")
    public DefaultGunData weaponData = null;
}
