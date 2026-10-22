package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Static link placement for one closed belt.
 *
 * The belt is built once, from the audited pins and the audited shoe box, and
 * every consumer reads this list: the geometry audit measures these boxes and
 * the `TrackRunSpec` adapter turns the same boxes into Bullet bodies. Deriving
 * the pose twice is how an adapter and an audit end up disagreeing.
 *
 * Frame construction:
 *
 * - The belt plane is the chassis `(z, y)` plane; `x` is the lateral axis.
 * - Each link rotates about `x` only, so its local `+Z` follows the chord
 *   between its two pins. Both ends therefore share the hinge axis.
 * - The pins are exactly half a pitch from the link's chord midpoint, so the
 *   pin-frame pivots are `(0, 0, ±pitch/2)` before the shoe offset is applied.
 * - The rigid body sits at the *shoe box centre*, which may be offset from the
 *   pin frame along the shoe's local axes.
 */
object TrackedAssemblyGeometry {

    /** Rotation about `x` that maps local `+Z` onto the chord direction. */
    fun chordRotation(pinStart: ClosedTrackLoopSolver.Pin, pinEnd: ClosedTrackLoopSolver.Pin): Float {
        val dz = pinEnd.z - pinStart.z
        val dy = pinEnd.y - pinStart.y
        return atan2(-dy, dz).toFloat()
    }

    fun rotationQuaternion(angleRad: Float): CompiledRotation {
        val half = angleRad.toDouble() * 0.5
        return CompiledRotation(
            x = kotlin.math.sin(half).toFloat(),
            y = 0f,
            z = 0f,
            w = kotlin.math.cos(half).toFloat(),
        )
    }

    /** Rotate a pin-frame point by [angleRad] about `x`. */
    fun rotateAboutX(point: Vec3, angleRad: Float): Vec3 {
        val c = cos(angleRad.toDouble())
        val s = sin(angleRad.toDouble())
        return Vec3(
            point.x,
            point.y * c - point.z * s,
            point.y * s + point.z * c,
        )
    }

    /**
     * Place every link of a compiled side.
     *
     * [centerX] is the belt's lateral centre from the link template bone; the
     * pin polygon only carries the `(z, y)` plane.
     */
    fun place(
        pins: List<ClosedTrackLoopSolver.Pin>,
        centerX: Double,
        boxCenterInPinFrame: Vec3,
        halfExtents: Vec3,
    ): List<CompiledLinkPlacement> {
        require(pins.size >= 4) { "a closed belt needs at least four pins, got ${pins.size}" }
        val out = ArrayList<CompiledLinkPlacement>(pins.size)
        for (index in pins.indices) {
            val start = pins[index]
            val end = pins[(index + 1) % pins.size]
            val chord = hypot(end.z - start.z, end.y - start.y)
            require(chord > 1.0e-6) { "pin $index and ${(index + 1) % pins.size} coincide" }

            val angle = chordRotation(start, end)
            val midpoint = Vec3(centerX, (start.y + end.y) * 0.5, (start.z + end.z) * 0.5)
            val offset = rotateAboutX(boxCenterInPinFrame, angle)
            out += CompiledLinkPlacement(
                index = index,
                pinStart = Vec3(centerX, start.y, start.z),
                pinEnd = Vec3(centerX, end.y, end.z),
                center = midpoint.add(offset),
                rotation = rotationQuaternion(angle),
                boxCenterInPinFrame = boxCenterInPinFrame,
                halfExtents = halfExtents,
                chordLength = chord,
            )
        }
        return out
    }

    /**
     * Signed distance between one shoe box and one wheel cylinder, metres.
     *
     * Both shapes are treated as their exact `(z, y)` cross-section: the shoe is
     * a rotated rectangle and the wheel a circle. Wheels that do not overlap the
     * shoe laterally cannot touch it and return [Double.POSITIVE_INFINITY].
     *
     * Negative means the collision shapes overlap.
     */
    fun shoeWheelGap(
        placement: CompiledLinkPlacement,
        shoeHalfWidth: Double,
        wheelCenter: Vec3,
        wheelRadius: Double,
        wheelHalfWidth: Double,
    ): Double {
        val lateralOverlap = minOf(
            placement.center.x + shoeHalfWidth,
            wheelCenter.x + wheelHalfWidth,
        ) - maxOf(
            placement.center.x - shoeHalfWidth,
            wheelCenter.x - wheelHalfWidth,
        )
        if (lateralOverlap <= 0.0) return Double.POSITIVE_INFINITY

        // Work in the link's own (z, y) section. The link rotation is about x,
        // so the shoe's local axes are the pin frame rotated by the same angle.
        val angle = chordAngleOf(placement)
        val dy = wheelCenter.y - placement.center.y
        val dz = wheelCenter.z - placement.center.z
        val localY = dy * cos(angle) + dz * sin(angle)
        val localZ = -dy * sin(angle) + dz * cos(angle)

        val edgeY = abs(localY) - placement.halfExtents.y
        val edgeZ = abs(localZ) - placement.halfExtents.z
        val distance = if (edgeY > 0.0 || edgeZ > 0.0) {
            hypot(maxOf(edgeY, 0.0), maxOf(edgeZ, 0.0))
        } else {
            // Inside the rectangle: the distance to the boundary is negative.
            maxOf(edgeY, edgeZ)
        }
        return distance - wheelRadius
    }

    /**
     * Recover the link's rotation about `x` from its stored quaternion.
     *
     * The placement is produced by this object, so the rotation is always a
     * pure `x` rotation; a general quaternion here would mean the caller built a
     * placement by hand and the section test no longer applies.
     */
    fun chordAngleOf(placement: CompiledLinkPlacement): Double {
        val r = placement.rotation
        require(r.y == 0f && r.z == 0f) {
            "link ${placement.index} is not a pure lateral rotation: $r"
        }
        return 2.0 * atan2(r.x.toDouble(), r.w.toDouble())
    }

    /** Angle between two consecutive chords, radians. */
    fun jointAngle(previous: CompiledLinkPlacement, next: CompiledLinkPlacement): Double {
        val delta = chordAngleOf(next) - chordAngleOf(previous)
        var wrapped = delta % (2.0 * PI)
        if (wrapped > PI) wrapped -= 2.0 * PI
        if (wrapped < -PI) wrapped += 2.0 * PI
        return wrapped
    }
}
