package com.atsuishio.superbwarfare.physics

import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelSide
import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelRole
import net.minecraft.world.phys.Vec3
import org.joml.Quaternionf

/** One fixed OBB component that belongs to the Bullet chassis shape. */
internal data class CollisionPart(
    val half: Vec3,
    /** Position relative to the collision reference and centre of mass. */
    val center: Vec3,
    val rotation: Quaternionf = Quaternionf(),
)

/**
 * One complete wheel group or track side in the chassis' local frame.
 * Coordinates are relative to the collision OBB centre and use the same axes
 * as the authored vehicle data: +X left, +Y up, +Z forward.
 */
internal data class SideSuspensionSpec(
    val side: WheelSide,
    /** Lateral centre of the continuous side support face. */
    val anchorX: Double,
    /** Bottom support plane in chassis-local coordinates. */
    val bottomY: Double,
    /** Centre of the longitudinal support footprint. */
    val centerZ: Double,
    /** Half width of the continuous bottom face. */
    val halfWidth: Double,
    /** Half length of the continuous bottom face. */
    val halfLength: Double,
) {
    val sign: Double get() = if (side == WheelSide.LEFT) 1.0 else -1.0
}

/** One normalized wheel or track station in the chassis-local frame. */
internal data class WheelStationSpec(
    val index: Int,
    val side: WheelSide,
    val role: WheelRole,
    val center: Vec3,
    val radius: Double,
    val halfWidth: Double,
    /** Longitudinal half extent used for side support probes. */
    val halfLength: Double = radius,
    val steerable: Boolean = false,
    val contactRadius: Double = radius,
)

/** Immutable wheel projection sent to the render thread. */
data class WheelRenderState(
    val index: Int,
    val side: WheelSide,
    val suspensionTravel: Float = 0f,
    val steeringAngle: Float = 0f,
    val spin: Float = 0f,
    val grounded: Boolean = false,
    /**
     * True when this station physically steers. Model bones for non-steering
     * wheels must keep following the vehicle rudder instead of a fixed zero.
     */
    val steerable: Boolean = false,
) {
    fun finite(): Boolean = suspensionTravel.isFinite() && steeringAngle.isFinite() && spin.isFinite()
}

/**
 * One articulated sub-body (the turret or the gun barrel).
 *
 * [pivot] is the revolute anchor expressed in the parent body's local frame and
 * every part centre is expressed relative to that same pivot, so the whole
 * structure can be built without referencing a live Entity.
 */
internal data class ArticulationSpec(
    val pivot: Vec3,
    val parts: List<CollisionPart>,
    /** Free rotation axis: 0 = X (barrel pitch), 1 = Y (turret yaw), 2 = Z. */
    val axis: Int,
    val lowerLimit: Double,
    val upperLimit: Double,
)

/**
 * Everything the Spark physics worker needs, captured on the game thread.
 *
 * Nothing in here references a Minecraft or Bullet object, so the worker can
 * consume it without touching the game thread's world.
 */
internal data class VehiclePhysicsSpec(
    val clientSide: Boolean,
    val half: Vec3,
    val localCenter: Vec3,
    val pivot: Double,
    val massKg: Float,
    val anchored: Boolean,
    val groundVehicle: Boolean,
    val trackedVehicle: Boolean,
    val flightVehicle: Boolean,
    val fixedWing: Boolean,
    val settings: VehiclePhysicsSettings,
    val collisionParts: List<CollisionPart>,
    val wheelStations: List<WheelStationSpec>,
    val suspensionSides: List<SideSuspensionSpec>,
    /** Constrained turret body, when the vehicle authors turret OBBs. */
    val turret: ArticulationSpec? = null,
    /** Constrained barrel body, when the vehicle authors barrel OBBs. */
    val barrel: ArticulationSpec? = null,
    val initial: RigidState,
    val diagnosticName: String = "vehicle",
    val initialTurretYaw: Float = 0f,
    val initialBarrelPitch: Float = 0f,
    val trackAnimationLength: Float = 100f,
) {
    fun side(side: WheelSide): SideSuspensionSpec? = suspensionSides.firstOrNull { it.side == side }
}
