package com.atsuishio.superbwarfare.physics.tracked.legacy

import cn.solarmoon.spark_core.physics.track.TrackRunJointSpec
import cn.solarmoon.spark_core.physics.track.TrackRunLinkShape
import cn.solarmoon.spark_core.physics.track.TrackRunLinkSpec
import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import com.atsuishio.superbwarfare.physics.tracked.TrackedGeometryAudit
import com.atsuishio.superbwarfare.physics.tracked.TrackedSide
import com.atsuishio.superbwarfare.physics.tracked.TrackedVehicleSpec
import net.minecraft.world.phys.Vec3

/**
 * The historical 79-shoe closed chain as a core `TrackRunSpec`.
 *
 * **Legacy topology.** This exists so the earlier S1 and B2.0 evidence stays
 * reproducible. It is not the physical mechanism any more and no new fixture may
 * be built on it.
 */
@Deprecated("Legacy 79-shoe chain; use TrackedRunSpecs.compileRuns")
object LegacyShoeChainRunSpecs {

    fun compile(spec: TrackedVehicleSpec, side: TrackedSide): TrackRunSpec {
        val compiled = spec.side(side)
        val legacy = compiled.requireLegacy()
        val placements = legacy.placements
        require(placements.size >= 4) { "$side loop has ${placements.size} links" }
        require(!legacy.geometry.interpenetrates) {
            "$side shoe/wheel assembly interpenetrates by " +
                "${"%.2f".format(-legacy.geometry.minShoeWheelGapM * 1000.0)} mm at link " +
                "${legacy.geometry.worstLinkIndex} / wheel ${legacy.geometry.worstWheelBone}"
        }

        val pitch = legacy.loop.pitch
        val halfPitch = (pitch * 0.5).toFloat()
        val halfExtents = legacy.linkTemplate.physical.halfExtents
        require(halfExtents.x > 0.0 && halfExtents.y > 0.0 && halfExtents.z > 0.0) {
            "$side link template has a degenerate extent: $halfExtents"
        }

        val halfAngleRad = Math.toRadians(spec.angularLimitDegrees).toFloat()
        val count = placements.size

        val links = ArrayList<TrackRunLinkSpec>(count)
        val joints = ArrayList<TrackRunJointSpec>(count)
        for (placement in placements) {
            val boxOffset = placement.boxCenterInPinFrame
            links += TrackRunLinkSpec(
                massKg = legacy.linkMassKg.toFloat(),
                shape = TrackRunLinkShape.Box(halfExtents),
                position = placement.center,
                rotationX = placement.rotation.x,
                rotationY = placement.rotation.y,
                rotationZ = placement.rotation.z,
                rotationW = placement.rotation.w,
            )
            // The body origin sits at the shoe box centre, so the pins must be
            // expressed relative to that offset, not to the pin-frame origin.
            joints += TrackRunJointSpec(
                previousIndex = placement.index,
                nextIndex = (placement.index + 1) % count,
                pivotInPrevious = Vec3(0.0, 0.0, halfPitch.toDouble()).subtract(boxOffset),
                pivotInNext = Vec3(0.0, 0.0, -halfPitch.toDouble()).subtract(boxOffset),
                axis = 0,
                halfAngleRad = halfAngleRad,
            )
        }

        return TrackRunSpec(
            name = "${spec.vehicleId}_track_${side.name.lowercase()}",
            links = links,
            joints = joints,
            friction = spec.drivetrain.friction.toFloat(),
            explicitGravity = null,
        )
    }

    /** True when the assembled belt fits the authored hinge limit. */
    fun wrapFits(spec: TrackedVehicleSpec, side: TrackedSide): Boolean =
        TrackedGeometryAudit.maxJointAngleDegrees(spec.side(side).requireLegacy().placements) <=
            spec.angularLimitDegrees + 1.0e-6
}
