package com.atsuishio.superbwarfare.data.gun;

import com.atsuishio.superbwarfare.data.ObjectToList;
import com.atsuishio.superbwarfare.data.StringOrVec3;
import com.google.gson.annotations.SerializedName;
import net.minecraft.world.phys.Vec3;

public class ShootPos {

    @SerializedName("Transform")
    public String transform = "Default";

    // 注意这个是复数
    @SerializedName("Positions")
    public ObjectToList<Vec3> positions = new ObjectToList<>(Vec3.ZERO);

    // TODO 把这个也改成List后好像炸了
    @SerializedName("Directions")
    public ObjectToList<StringOrVec3> directions = new ObjectToList<>(new StringOrVec3("Default"));
}
