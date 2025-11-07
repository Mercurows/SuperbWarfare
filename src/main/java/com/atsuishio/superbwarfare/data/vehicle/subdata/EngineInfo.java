package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.google.gson.annotations.SerializedName;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public class EngineInfo {

    // 能量消耗比例
    @SerializedName("EnergyCostRate")
    public double energyCostRate = 1;
    // 浮力，大于零时认为载具是两栖的
    @SerializedName("Buoyancy")
    public double buoyancy = 0;
    // 前进加速度
    @SerializedName("Increment")
    public float increment = 0.001f;
    // 后退加速度
    @SerializedName("Decrement")
    public float decrement = 0.001f;

    public static class Wheel extends EngineInfo {
        @SerializedName("WheelRotSpeed")
        public double wheelRotSpeed = 0;
        @SerializedName("WheelDifferential")
        public double wheelDifferential = 0;
        // 转向速度
        @SerializedName("SteeringSpeed")
        public float steeringSpeed = 0.1f;
        // 最大前进速度系数
        @SerializedName("MaxForwardSpeedRate")
        public float maxForwardSpeedRate = 0.2f;
        // 最大后腿速度系数
        @SerializedName("MaxBackwardSpeedRate")
        public float maxBackwardSpeedRate = -0.1f;
    }

    public static class Track extends Wheel {
        @SerializedName("TrackRotSpeed")
        public double trackRotSpeed = 0;
        @SerializedName("TrackDifferential")
        public double trackDifferential = 0;
    }

    public static class Helicopter extends EngineInfo {
        @SerializedName("PitchSpeed")
        public float pitchSpeed = 0;
        @SerializedName("YawSpeed")
        public float yawSpeed = 0;
        @SerializedName("RollSpeed")
        public float rollSpeed = 0;
        @SerializedName("LiftSpeed")
        public float liftSpeed = 0;

        // 引擎启动音效
        @SerializedName("EngineStartSound")
        public SoundEvent engineStartSound = SoundEvents.EMPTY;
    }
}
