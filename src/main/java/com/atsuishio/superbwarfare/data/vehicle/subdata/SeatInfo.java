package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.annotation.ServerOnly;
import com.atsuishio.superbwarfare.data.ObjectToList;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class SeatInfo {
    @SerializedName("HidePassenger")
    public boolean hidePassenger = false;

    @SerializedName("IsEnclosed")
    @ServerOnly
    public Boolean isEnclosed = null;

    @SerializedName("Transform")
    public String transform = "Default";

    @SerializedName("Pose")
    public String pose = "Default";

    @SerializedName("Position")
    public Vec3 position = Vec3.ZERO;

    @SerializedName("Orientation")
    public float orientation = 0;

    @SerializedName("CanRotateBody")
    public Boolean canRotateBody = false;

    @SerializedName("CanRotateHead")
    public Boolean canRotateHead = true;

    @SerializedName("HasThermalImaging")
    public Boolean hasThermalImaging = true;

    @SerializedName("MinPitch")
    public float minPitch = -90;

    @SerializedName("MaxPitch")
    public float maxPitch = 90;

    @SerializedName("MinYaw")
    public float minYaw = -514;

    @SerializedName("MaxYaw")
    public float maxYaw = 514;

    @SerializedName("Weapons")
    protected ObjectToList<String> weapons = new ObjectToList<>();

    public List<String> weapons() {
        if (weapons == null || weapons.list == null) return List.of();
        return weapons.list;
    }

    @SerializedName("CameraPos")
    public CameraPos cameraPos = null;

    @SerializedName("BanHand")
    public Boolean banHand = false;

    @SerializedName("Sensitivity")
    public Vec3 sensitivity = new Vec3(1, 1, 1);

    @SerializedName("DismountInfo")
    public DismountInfo dismountInfo = null;
}
