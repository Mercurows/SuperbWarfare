package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.gun.CameraPos;
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
    public float orientation = 0;

    @SerializedName("CanRotateBody")
    public Boolean canRotateBody = false;

    @SerializedName("CanRotateHead")
    public Boolean canRotateHead = true;

    @SerializedName("MinPitch")
    public float minPitch = -90;

    @SerializedName("MaxPitch")
    public float maxPitch = 90;

    @SerializedName("MinYaw")
    public float minYaw = -514;

    @SerializedName("MaxYaw")
    public float maxYaw = 514;

    @SerializedName("WeaponData")
    public DefaultGunData weaponData = null;

    @SerializedName("CameraPos")
    public CameraPos cameraPos = new CameraPos();
}
