package com.atsuishio.superbwarfare.data.vehicle.subdata;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils;
import com.google.gson.annotations.SerializedName;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

public abstract class EngineInfo {

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
    @SerializedName("EngineSoundVolume")
    public float engineSoundVolume = 0.4f;

    abstract public void work(VehicleEntity vehicle);

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
        // 最大后退速度系数
        @SerializedName("MaxBackwardSpeedRate")
        public float maxBackwardSpeedRate = -0.1f;

        @Override
        public void work(VehicleEntity vehicle) {
            VehicleEngineUtils.wheelEngine(vehicle, this);
        }
    }

    public static class Track extends Wheel {
        @SerializedName("TrackRotSpeed")
        public double trackRotSpeed = 0;
        @SerializedName("TrackDifferential")
        public double trackDifferential = 0;

        @Override
        public void work(VehicleEntity vehicle) {
            VehicleEngineUtils.trackEngine(vehicle, this);
        }
    }

    public static class WheelChair extends Wheel {
        @SerializedName("BodyRollRate")
        public double bodyRollRate = 1;
        @SerializedName("CanJump")
        public boolean canJump = false;
        @SerializedName("JumpEnergyCost")
        public int jumpEnergyCost = 400;
        @SerializedName("JumpCoolDown")
        public int jumpCoolDown = 3;
        @SerializedName("JumpForce")
        public double jumpForce = 0.6;

        @Override
        public void work(VehicleEntity vehicle) {
            VehicleEngineUtils.wheelChairEngine(vehicle, this);
        }
    }

    public static class Ship extends EngineInfo {
        @SerializedName("BodyPitchRate")
        public double bodyPitchRate = 1;
        @SerializedName("BodyRollRate")
        public double bodyRollRate = 1;
        // 转向速度
        @SerializedName("SteeringSpeed")
        public float steeringSpeed = 0.1f;
        // 最大前进速度系数
        @SerializedName("MaxForwardSpeedRate")
        public float maxForwardSpeedRate = 0.2f;
        // 最大后退速度系数
        @SerializedName("MaxBackwardSpeedRate")
        public float maxBackwardSpeedRate = -0.1f;

        @Override
        public void work(VehicleEntity vehicle) {
            VehicleEngineUtils.shipEngine(vehicle, this);
        }
    }

    public static class Helicopter extends EngineInfo {
        @SerializedName("PitchSpeed")
        public float pitchSpeed = 1;
        @SerializedName("YawSpeed")
        public float yawSpeed = 1;
        @SerializedName("RollSpeed")
        public float rollSpeed = 1;
        @SerializedName("LiftSpeed")
        public float liftSpeed = 1;

        @SerializedName("Speed")
        public float speed = 1;

        // 引擎启动音效
        @SerializedName("EngineStartSound")
        public SoundEvent engineStartSound = SoundEvents.EMPTY;

        @Override
        public void work(VehicleEntity vehicle) {
            VehicleEngineUtils.helicopterEngine(vehicle, this);
        }
    }

    public static class Aircraft extends Helicopter {
        @SerializedName("SpeedRate")
        public float speedRate = 1;
        @SerializedName("GearRotateAngle")
        public float gearRotateAngle = 85;
        @SerializedName("HasGear")
        public boolean hasGear = true;

        @SerializedName("Resistance")
        public float resistance = 1;

        @Override
        public void work(VehicleEntity vehicle) {
            VehicleEngineUtils.aircraftEngine(vehicle, this);
        }
    }

    public static class Tom6 extends Aircraft {
        @Override
        public void work(VehicleEntity vehicle) {
            VehicleEngineUtils.tomEngine(vehicle, this);
        }
    }
}
