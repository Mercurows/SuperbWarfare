package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

// TODO 摄像机位置可能不应该写到枪里，因为没武器的乘客也有可能需要这个
public class CameraPos {

    @SerializedName("Transform")
    public String transform = "Default";

    @SerializedName("Position")
    public Vec3 position = Vec3.ZERO;

    @SerializedName("Direction")
    public StringOrVec3 direction = new StringOrVec3("Default");

    @SerializedName("ZoomPosition")
    public Vec3 zoomPosition = Vec3.ZERO;

    @SerializedName("ZoomDirection")
    public StringOrVec3 zoomDirection = direction;

    @SerializedName("UseFixedCameraPos")
    public Boolean useFixedCameraPos = false;

    @SerializedName("UseSimulate3P")
    public Boolean useSimulate3P = false;

    @SerializedName("Simulate3PPos")
    public Vec2 simulate3PPos = new Vec2(6, 1);

    @SerializedName("AircraftCamera")
    public Boolean aircraftCamera = false;
}
