package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.google.gson.annotations.SerializedName;

public class EngineInfo {

    @SerializedName("Type")
    public Type type = Type.EMPTY;
    // 浮力，大于零时认为载具是两栖的
    @SerializedName("Buoyancy")
    public double buoyancy = 0;
    // 能量消耗比例
    @SerializedName("EnergyCostRate")
    public double energyCostRate = 1;
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
        // 最大前进速度系数
        @SerializedName("MaxForwardSpeedRate")
        public float maxForwardSpeedRate = 0.2f;
        // 最大后腿速度系数
        @SerializedName("MaxBackwardSpeedRate")
        public float maxBackwardSpeedRate = -0.1f;
        // 前进加速度
        @SerializedName("Increment")
        public float increment = 0.001f;
        // 后退加速度
        @SerializedName("Decrement")
        public float decrement = 0.001f;
    }
}
