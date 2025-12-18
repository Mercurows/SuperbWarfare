package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

public class DismountInfo {

    @SerializedName("Transform")
    public String transform = "Default";

    @SerializedName("Position")
    public Vec3 position = null;

    // 能否弹射成员
    @SerializedName("CanEject")
    public boolean canEject;
    @SerializedName("EjectPosition")
    public Vec3 ejectPosition = null;

    @SerializedName("EjectDirection")
    public StringOrVec3 ejectDirection = new StringOrVec3("Up");

    @SerializedName("EjectForce")
    public double ejectForce = 2;
}
