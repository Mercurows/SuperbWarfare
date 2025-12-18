package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class CameraPos {

    @SerializedName("Transform")
    public String transform = "Default";

    @SerializedName("Position")
    public Vec3 position = Vec3.ZERO;

    @SerializedName("Direction")
    public StringOrVec3 direction = new StringOrVec3("Default");

    @SerializedName("ZoomPosition")
    public Vec3 zoomPosition = null;

    @SerializedName("ZoomDirection")
    public StringOrVec3 zoomDirection = null;

    @SerializedName("UseFixedCameraPos")
    public boolean useFixedCameraPos = false;

    @SerializedName("UseSimulate3P")
    public boolean useSimulate3P = false;

    @SerializedName("Simulate3PPos")
    public Vec2 simulate3PPos = new Vec2(6, 1);

    @SerializedName("UseAircraftCamera")
    public boolean useAircraftCamera = false;

    @SerializedName("AircraftCameraPos")
    public Vec3 aircraftCameraPos = new Vec3(0, 3, -10);
}
