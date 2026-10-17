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
    @SerialName("SuspensionVisualScale") val suspensionVisualScale: Float = 1f,
    @SerialName("SuspensionVisualMaxOffset") val suspensionVisualMaxOffset: Float = .3f,
    @SerialName("TrackPhaseScale") val trackPhaseScale: Float = 1f,
    @SerialName("TrackRollingResistance") val trackRollingResistance: Float = .015f,
    /** Reserved from authored mass for the two continuous support plates. */
    @SerialName("TrackSupportMassFraction") val trackSupportMassFraction: Float = .08f,
    /** Explicit whole-side sprung-mode ratios, converted to SI c=2*zeta*m*omega.
     * WHEEL retains its existing per-wheel SI compression/rebound coefficients.
     */
    @SerialName("TrackCompressionDampingRatio") val trackCompressionDampingRatio: Float = .65f,
    @SerialName("TrackReboundDampingRatio") val trackReboundDampingRatio: Float = .45f,
    /** Hard per-side suspension row limit in N, never raised to fit weight.
     * Bump-stop/contact constraints remain separate from the elastic/viscous row.
     */
    @SerialName("TrackMaxSuspensionForce") val trackMaxSuspensionForce: Float = 1_000_000f,
    /** Authored pivot/turn limiter: the differential steering tracks this yaw
     * rate (rad/s) instead of a raw differential demand, so a tracked vehicle
     * can never spin up to friction-saturation. */
    @SerialName("TrackMaxYawRate") val trackMaxYawRate: Float = .8f,
    /** Yaw-rate error gain, relative to the track half-width. */
    @SerialName("TrackYawResponse") val trackYawResponse: Float = 0.6f,
    /** First-order smoothing time (s) applied to the commanded track speeds. */
    @SerialName("TrackYawSmoothing") val trackYawSmoothing: Float = .3f,
    /** Maximum controlled yaw-rate change in rad/s^2. */
    @SerialName("TrackMaxYawAcceleration") val trackMaxYawAcceleration: Float = 4f,
    @SerialName("MaxSteeringTorque") val maxSteeringTorque: Float = 5_000_000f,
    @SerialName("AngularVelocityLimit") val angularVelocityLimit: Float = 6f,
    @SerialName("AngularVelocityDamping") val angularVelocityDamping: Float = 2f,
    @SerialName("TireLongitudinalStiffness") val tireLongitudinalStiffness: Float = 18000f,
    @SerialName("TireLateralStiffness") val tireLateralStiffness: Float = 22000f,
    @SerialName("WheelDriveTorque") val wheelDriveTorque: Float = 180000f,
    @SerialName("WheelSteeringAngle") val wheelSteeringAngle: Float = 0.65f,
    @SerialName("WheelSteeringForce") val wheelSteeringForce: Float = 250000f,
    @SerialName("WheelMassFraction") val wheelMassFraction: Float = 0.08f,
    /** Tire/bearing rolling resistance for WHEEL bodies. Native Bullet has no
     * rolling friction here, so without it a free wheel coasts forever and a
     * settling vehicle can never come to rest. */
    @SerialName("WheelRollingResistance") val wheelRollingResistance: Float = .015f,
    @SerialName("ArticulationEnabled") val articulationEnabled: Boolean = true,
    @SerialName("ArticulationMassFraction") val articulationMassFraction: Float = 0.03f,
    @SerialName("ArticulationServoForce") val articulationServoForce: Float = 200000f,
)
