package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.airShipEngine
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.aircraftEngine
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.helicopterEngine
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.shipEngine
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.tomEngine
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.trackEngine
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.wheelChairEngine
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleEngineUtils.wheelEngine
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedSoundEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.sounds.SoundEvents

@Serializable
abstract class EngineInfo {
    // 能量消耗比例
    @SerialName("EnergyCostRate")
    var energyCostRate: Double = 1.0

    // 浮力，大于零时认为载具是两栖的
    @SerialName("Buoyancy")
    var buoyancy: Double = 0.0

    // 前进加速度
    @SerialName("Increment")
    var increment: Float = 0.001f

    // 后退加速度
    @SerialName("Decrement")
    var decrement: Float = 0.001f

    @SerialName("EngineSoundVolume")
    var engineSoundVolume: Float = 0.4f

    @SerialName("SprintMultiply")
    var sprintMultiply: Double = 1.0

    abstract fun work(vehicle: VehicleEntity)

    @Serializable
    open class AirShip : EngineInfo() {
        // 转向速度
        @SerialName("SteeringSpeed")
        var steeringSpeed: Float = 1f

        // 最大前进速度系数
        @SerialName("MaxForwardSpeedRate")
        var maxForwardSpeedRate: Float = 1f

        // 最大后退速度系数
        @SerialName("MaxBackwardSpeedRate")
        var maxBackwardSpeedRate: Float = 1f

        // 最大上升速度系数
        @SerialName("MaxUpSpeedRate")
        var maxUpSpeedRate: Float = 1f

        // 最大上升速度系数
        @SerialName("MaxDownSpeedRate")
        var maxDownSpeedRate: Float = 1f

        @SerialName("FloatHeight")
        var floatHeight: Double = 3.0

        override fun work(vehicle: VehicleEntity) {
            vehicle.airShipEngine(this)
        }
    }

    @Serializable
    open class Wheel : EngineInfo() {
        @SerialName("WheelRotSpeed")
        var wheelRotSpeed: Double = 0.0

        @SerialName("WheelDifferential")
        var wheelDifferential: Double = 0.0

        // 转向速度
        @SerialName("SteeringSpeed")
        var steeringSpeed: Float = 0.1f

        // 最大前进速度系数
        @SerialName("MaxForwardSpeedRate")
        var maxForwardSpeedRate: Float = 0.2f

        // 最大后退速度系数
        @SerialName("MaxBackwardSpeedRate")
        var maxBackwardSpeedRate: Float = -0.1f

        override fun work(vehicle: VehicleEntity) {
            vehicle.wheelEngine(this)
        }
    }

    @Serializable
    class Track : Wheel() {
        @SerialName("TrackRotSpeed")
        var trackRotSpeed: Double = 0.0

        @SerialName("TrackDifferential")
        var trackDifferential: Double = 0.0

        override fun work(vehicle: VehicleEntity) {
            vehicle.trackEngine(this)
        }
    }

    @Serializable
    class WheelChair : Wheel() {
        @SerialName("CanJump")
        var canJump: Boolean = false

        @SerialName("JumpEnergyCost")
        var jumpEnergyCost: Int = 400

        @SerialName("JumpCoolDown")
        var jumpCoolDown: Int = 3

        @SerialName("JumpForce")
        var jumpForce: Double = 0.6

        override fun work(vehicle: VehicleEntity) {
            vehicle.wheelChairEngine(this)
        }
    }

    @Serializable
    class Ship : EngineInfo() {
        @SerialName("BodyPitchRate")
        var bodyPitchRate: Double = 1.0

        // 转向速度
        @SerialName("SteeringSpeed")
        var steeringSpeed: Float = 0.1f

        // 最大前进速度系数
        @SerialName("MaxForwardSpeedRate")
        var maxForwardSpeedRate: Float = 0.2f

        // 最大后退速度系数
        @SerialName("MaxBackwardSpeedRate")
        var maxBackwardSpeedRate: Float = -0.1f

        override fun work(vehicle: VehicleEntity) {
            vehicle.shipEngine(this)
        }
    }

    @Serializable
    open class Helicopter : EngineInfo() {
        @SerialName("PitchSpeed")
        var pitchSpeed: Float = 1f

        @SerialName("YawSpeed")
        var yawSpeed: Float = 1f

        @SerialName("RollSpeed")
        var rollSpeed: Float = 1f

        @SerialName("LiftSpeed")
        var liftSpeed: Float = 1f

        @SerialName("Speed")
        var speed: Float = 1f

        @SerialName("ClampPitch")
        var clampPitch: Float = 514f

        @SerialName("ClampRoll")
        var clampRoll: Float = 514f

        // 引擎启动音效
        @SerialName("EngineStartSound")
        var engineStartSound: SerializedSoundEvent = SoundEvents.EMPTY

        override fun work(vehicle: VehicleEntity) {
            vehicle.helicopterEngine(this)
        }
    }

    @Serializable
    open class Aircraft : Helicopter() {
        @SerialName("SpeedRate")
        var speedRate: Float = 1f

        @SerialName("GearRotateAngle")
        var gearRotateAngle: Float = 85f

        @SerialName("HasGear")
        var hasGear: Boolean = true

        @SerialName("HasStukaSound")
        var hasStukaSound: Boolean = false

        @SerialName("Resistance")
        var resistance: Float = 1f

        override fun work(vehicle: VehicleEntity) {
            vehicle.aircraftEngine(this)
        }
    }

    @Serializable
    class Tom6 : Aircraft() {
        override fun work(vehicle: VehicleEntity) {
            vehicle.tomEngine(this)
        }
    }
}
