package com.atsuishio.superbwarfare.physics

import com.atsuishio.superbwarfare.data.vehicle.subdata.WheelSide
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
 * as the rendered model: +X right, +Y up, +Z forward.
 */
internal data class SideSuspensionSpec(
    val side: WheelSide,
    /** Lateral position of the side's single force anchor. */
    val anchorX: Double,
    /** Bottom support plane in chassis-local coordinates. */
    val bottomY: Double,
    /** Centre of the longitudinal support footprint. */
    val centerZ: Double,
    /** Half width of the entire side probe. */
    val halfWidth: Double,
    /** Half length of the entire side probe. */
    val halfLength: Double,
) {
    val sign: Double get() = if (side == WheelSide.LEFT) -1.0 else 1.0
}

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
    val suspensionSides: List<SideSuspensionSpec>,
    val initial: RigidState,
) {
    fun side(side: WheelSide): SideSuspensionSpec? = suspensionSides.firstOrNull { it.side == side }
}
