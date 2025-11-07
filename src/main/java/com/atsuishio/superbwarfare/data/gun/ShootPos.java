package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class ShootPos {

    @SerializedName("Transform")
    public String transform = "Default";

    // 注意这个是复数
    // TODO 允许普通枪使用Positions
    @SerializedName("Positions")
    public List<Vec3> positions = List.of(Vec3.ZERO);

    // TODO 允许普通枪使用Directions
    @SerializedName("Directions")
    public List<StringOrVec3> directions = List.of(new StringOrVec3("Default"));

    @SerializedName("ViewDirection")
    public StringOrVec3 viewDirection = null;
}
