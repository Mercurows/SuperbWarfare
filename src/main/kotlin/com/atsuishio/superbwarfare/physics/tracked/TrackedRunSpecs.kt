package com.atsuishio.superbwarfare.physics.tracked

import cn.solarmoon.spark_core.physics.track.TrackRunJointSpec
import cn.solarmoon.spark_core.physics.track.TrackRunLinkShape
import cn.solarmoon.spark_core.physics.track.TrackRunLinkSpec
import cn.solarmoon.spark_core.physics.track.TrackRunSpec
import net.minecraft.world.phys.Vec3

/**
 * Turns one audited [CompiledTrackSide] into a generic core `TrackRunSpec`.
 *
 * This is pure data: it creates no Bullet object and runs on any thread. The
 * runtime assembly, the wheel-to-link contacts and the drive coupling are later
 * stages; S1 only has to prove that the authored mechanism can be expressed as
 * a closed chain of links with the audited pitch, mass and hinge limit.
 *
 * Link poses come from [TrackedAssemblyGeometry], the same list the static
 * interference audit measures. The adapter never re-derives a pose, so a shoe
 * that fails the audit cannot be assembled into a different, apparently valid
 * shape here.
 */
object TrackedRunSpecs {
    /**
     * Build the physical run for [side].
     *
     * Throws when the compiled side is not assemblable. A mechanism error is
     * reported at compile time, and [TrackedCompilation.requireSpec] is the
     * intended entry point; this check is the last line of defence.
     */
    fun compile(spec: TrackedVehicleSpec, side: TrackedSide): TrackRunSpec {
        val compiled = spec.side(side)
        val placements = compiled.placements
        require(placements.size >= 4) { "$side loop has ${placements.size} links" }
        require(!compiled.geometry.interpenetrates) {
            "$side shoe/wheel assembly interpenetrates by " +
                "${"%.2f".format(-compiled.geometry.minShoeWheelGapM * 1000.0)} mm at link " +
                "${compiled.geometry.worstLinkIndex} / wheel ${compiled.geometry.worstWheelBone}"
        }

        val pitch = compiled.loop.pitch
        val halfPitch = (pitch * 0.5).toFloat()
        val halfExtents = compiled.linkTemplate.physical.halfExtents
        val halfWidth = halfExtents.x.toFloat()
        val halfThickness = halfExtents.y.toFloat()
        val halfLength = halfExtents.z.toFloat()
        require(halfWidth > 0f && halfThickness > 0f && halfLength > 0f) {
            "$side link template has a degenerate extent: $halfExtents"
        }

        val halfAngleRad = Math.toRadians(spec.angularLimitDegrees).toFloat()
        val count = placements.size

        val links = ArrayList<TrackRunLinkSpec>(count)
        val joints = ArrayList<TrackRunJointSpec>(count)
        for (placement in placements) {
            val boxOffset = placement.boxCenterInPinFrame
            links += TrackRunLinkSpec(
                massKg = spec.linkMassKg.toFloat(),
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
        TrackedGeometryAudit.maxJointAngleDegrees(spec.side(side).placements) <=
            spec.angularLimitDegrees + 1.0e-6
}
