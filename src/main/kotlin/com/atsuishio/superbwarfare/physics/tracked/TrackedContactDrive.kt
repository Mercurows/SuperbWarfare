package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3
import kotlin.math.min

/**
 * Bounds a contact-motion command by the authored torque.
 *
 * Contact motion is a target surface speed, not a force. The Coulomb coefficient
 * written on that contact is what limits the tangential impulse. This keeps
 * `MaxDriveTorque` / `MaxBrakeTorque` from being ignored while still refusing to
 * apply a net force to the chassis or a fixed wheel.
 */
internal object TrackedContactDrive {
    fun frictionCoefficient(
        authoredFriction: Double,
        torqueNm: Double,
        radiusM: Double,
        supportedMassKg: Double,
        gravity: Double = 9.81,
    ): Double {
        if (!authoredFriction.isFinite() || authoredFriction <= 0.0) return 0.0
        val authored = authoredFriction.coerceIn(0.0, 10.0)
        if (!torqueNm.isFinite() || torqueNm <= 0.0 || !radiusM.isFinite() || radiusM <= 0.05 ||
            !supportedMassKg.isFinite() || supportedMassKg <= 0.0 || !gravity.isFinite() || gravity <= 0.0
        ) {
            return authored
        }
        val muFromTorque = (torqueNm / radiusM) / (supportedMassKg * gravity)
        if (!muFromTorque.isFinite() || muFromTorque <= 0.0) return 0.0
        return min(authored, muFromTorque).coerceIn(0.0, 10.0)
    }

    /**
     * Orthonormal pair of directions Bullet uses for the two friction rows of a
     * contact point.
     *
     * Both directions must lie in the real contact plane and be perpendicular to
     * each other. The previous code projected the chassis forward onto the plane
     * and then crossed it with the normal. That is degenerate in exactly the case
     * the step assist exists for: driving straight at a vertical riser makes the
     * forward projection zero, the fallback kept a vector parallel to the normal,
     * and `normal × tangent` collapsed to a zero "lateral" axis. Writing such a
     * frame asked the solver to build friction rows along the normal.
     *
     * Returns `null` when [preferred] has no usable in-plane component. The
     * caller must then leave the contact alone: inventing a direction would let
     * an unassisted belt crawl up a face it is only pressed against.
     */
    fun contactFrame(normal: Vec3, preferred: Vec3): ContactFrame? {
        if (!normal.x.isFinite() || !normal.y.isFinite() || !normal.z.isFinite()) return null
        if (!preferred.x.isFinite() || !preferred.y.isFinite() || !preferred.z.isFinite()) return null
        if (normal.lengthSqr() < 1.0e-12) return null
        val n = normal.normalize()
        val projected = preferred.subtract(n.scale(preferred.dot(n)))
        if (projected.lengthSqr() < 1.0e-10) return null
        val dir1 = projected.normalize()
        val dir2 = n.cross(dir1)
        if (dir2.lengthSqr() < 1.0e-10) return null
        return ContactFrame(dir1, dir2.normalize())
    }

    /**
     * Unit in-plane direction of [preferred], or `null` when there is none.
     *
     * Separate from [contactFrame] because a caller may need the drive
     * direction before it knows which contact frame to write.
     */
    fun projectOntoPlane(normal: Vec3, preferred: Vec3): Vec3? {
        if (!normal.x.isFinite() || !normal.y.isFinite() || !normal.z.isFinite()) return null
        if (!preferred.x.isFinite() || !preferred.y.isFinite() || !preferred.z.isFinite()) return null
        if (normal.lengthSqr() < 1.0e-12) return null
        val n = normal.normalize()
        val projected = preferred.subtract(n.scale(preferred.dot(n)))
        return if (projected.lengthSqr() < 1.0e-10) null else projected.normalize()
    }

    /**
     * Candidate free-roll decay for `RollingResistance`.
     *
     * **Not called by the runtime.** Two velocity-target implementations were
     * measured in a GameTest and both injected energy into this mechanism: a
     * target based on the run's own surface speed braked the hull hard, and a
     * target based on the hull's speed acted as a throttle and made the vehicle
     * accelerate after the driver released the controls. The maths is kept only
     * as a documented candidate for a future contact model that can express a
     * passive loss; `RollingResistance` is therefore **not implemented**.
     *
     * This is a **speed decay**, not a friction coefficient: MCSP's
     * `RollingLoss` is a fraction of the free rolling speed, not Coulomb
     * contact friction.
     *
     * The exponent is bounded: a non-positive or non-finite loss returns the
     * current speed unchanged, and an absurd loss is clamped so one substep can
     * never reverse the belt.
     */
    fun coastTarget(
        currentSurfaceSpeedMps: Double,
        rollingLoss: Double,
        tps: Double = 60.0,
    ): Double {
        if (!currentSurfaceSpeedMps.isFinite()) return 0.0
        if (!rollingLoss.isFinite() || rollingLoss <= 0.0) return currentSurfaceSpeedMps
        if (!tps.isFinite() || tps <= 0.0) return currentSurfaceSpeedMps
        val loss = rollingLoss.coerceIn(0.0, 1.0)
        val factor = kotlin.math.exp(-loss / tps)
        if (!factor.isFinite() || factor <= 0.0 || factor > 1.0) return currentSurfaceSpeedMps
        val decayed = currentSurfaceSpeedMps * factor
        return if (!decayed.isFinite()) 0.0 else decayed
    }
}

/** Two orthonormal, in-plane directions for Bullet's friction rows. */
internal data class ContactFrame(val dir1: Vec3, val dir2: Vec3)
