package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.google.gson.annotations.SerializedName;

public class EngineInfo {

    @SerializedName("Type")
    public Type type = Type.EMPTY;
    // 浮力，大于零时认为载具是两栖的
    @SerializedName("Buoyancy")
    public double buoyancy = 0;
    // TODO 允许这里添加 @power 之类的变量，便于动态计算能耗
    // 能量消耗
    @SerializedName("EnergyCost")
    public int energyCost = 0;
    // 车轮控制相关
    @SerializedName("Wheel")
    public WheelInfo wheel;
    // 履带控制相关
    @SerializedName("Track")
    public TrackInfo track;
    // 功率
    @SerializedName("Power")
    public PowerInfo power;
    // 转向速度
    @SerializedName("SteeringSpeed")
    public float steeringSpeed;

    public enum Type {
        @SerializedName("Empty") EMPTY,
        @SerializedName("Wheel") WHEEL,
        @SerializedName("Track") TRACK,
    }

    public static class WheelInfo {
        @SerializedName("RotSpeed")
        public double rotSpeed = 0;
        @SerializedName("Differential")
        public double differential = 0;
    }

    public static class TrackInfo {
        @SerializedName("RotSpeed")
        public double rotSpeed = 0;
        @SerializedName("Differential")
        public double differential = 0;
    }

    public static class PowerInfo {
        @SerializedName("MaxPower")
        public float maxPower = 0.2f;
        @SerializedName("MinPower")
        public float minPower = 0;
        @SerializedName("Increment")
        public float increment;
        @SerializedName("Decrement")
        public float decrement;
    }
}
