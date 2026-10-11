package com.atsuishio.superbwarfare.physics

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@Serializable
data class VehiclePhysicsSettings(
    @SerialName("CenterOfMassOffset") val centerOfMassOffset: SerializedVec3 = Vec3.ZERO,
    @SerialName("Gravity") val gravity: Double = 9.81,
    @SerialName("Friction") val friction: Float = 0.7f,
    @SerialName("Restitution") val restitution: Float = 0.05f,
    @SerialName("LinearDamping") val linearDamping: Float = 0.01f,
    @SerialName("AngularDamping") val angularDamping: Float = 0.15f,
    @SerialName("DriveScale") val driveScale: Double = 1.0,
    @SerialName("SteeringScale") val steeringScale: Double = 1.0,
    @SerialName("RecoilScale") val recoilScale: Double = 1000.0,
    @SerialName("ExplosionScale") val explosionScale: Double = 100.0,
    @SerialName("SuspensionEnabled") val suspensionEnabled: Boolean = true,
    @SerialName("SuspensionTravelUp") val suspensionTravelUp: Float = 0.18f,
    @SerialName("SuspensionTravelDown") val suspensionTravelDown: Float = 0.24f,
    @SerialName("SuspensionFrequency") val suspensionFrequency: Float = 2.2f,
    @SerialName("SuspensionCompressionDamping") val suspensionCompressionDamping: Float = 1200f,
    @SerialName("SuspensionReboundDamping") val suspensionReboundDamping: Float = 700f,
    @SerialName("SuspensionAntiRoll") val suspensionAntiRoll: Float = 0.65f,
    @SerialName("SuspensionMaxForce") val suspensionMaxForce: Float = 250000f,
    @SerialName("SuspensionMaxSupportAcceleration") val suspensionMaxSupportAcceleration: Float = 2.5f,
    @SerialName("SuspensionCompressionVelocityLimit") val suspensionCompressionVelocityLimit: Float = 8f,
    @SerialName("SuspensionSideOffset") val suspensionSideOffset: Float = 0.72f,
    @SerialName("SuspensionVisualScale") val suspensionVisualScale: Float = .25f,
    @SerialName("SuspensionVisualMaxOffset") val suspensionVisualMaxOffset: Float = .06f,
    @SerialName("TrackPhaseScale") val trackPhaseScale: Float = 1f,
    @SerialName("MaxSteeringTorque") val maxSteeringTorque: Float = 5_000_000f,
    @SerialName("AngularVelocityLimit") val angularVelocityLimit: Float = 6f,
    @SerialName("AngularVelocityDamping") val angularVelocityDamping: Float = 2f,
)
