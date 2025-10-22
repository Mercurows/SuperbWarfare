package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

public class ZoomPos {

    @SerializedName("Transform")
    public String transform = "Default";

    @SerializedName("Position")
    public Vec3 position = Vec3.ZERO;

    @SerializedName("Direction")
    public StringOrVec3 direction = new StringOrVec3("Default");
}
