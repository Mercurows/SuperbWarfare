package com.atsuishio.superbwarfare.physics.tracked

import kotlin.math.min

/**
 * Solver-stability envelope for a suspension spring row.
 *
 * A `New6Dof` spring is integrated explicitly inside the constraint solver, so
 * an arbitrary stiffness/damping pair diverges once it leaves the stable region.
 * The bounds below are the ones Machine-Max applies to its own joint springs:
 *
 *     k <= 4 * m_eff / dt^2
 *     c <= ((4 - k*dt^2/m_eff) / 2) * m_eff / dt
 *
 * `m_eff` is the *reduced mass of the two joined rigid bodies* (hull and wheel),
 * not the sprung mass carried by the wheel. Using the sprung mass instead makes
 * the bound about an order of magnitude too generous, which is exactly how the
 * first version of this rig ended up over-damped by ~2.5x and jittering.
 */
object TrackedStability {

    data class Result(
        val stiffnessNPerM: Double,
        val dampingNsPerM: Double,
        val maxStiffnessNPerM: Double,
        val maxDampingNsPerM: Double,
        /** True when either value had to be reduced. */
        val limited: Boolean,
    ) {
        val stiffnessRatio: Double get() = if (maxStiffnessNPerM > 0) stiffnessNPerM / maxStiffnessNPerM else 0.0
        val dampingRatio: Double get() = if (maxDampingNsPerM > 0) dampingNsPerM / maxDampingNsPerM else 0.0
    }

    private const val SAFETY = 0.75

    fun clamp(
        stiffnessNPerM: Double,
        dampingNsPerM: Double,
        effectiveMassKg: Double,
        dt: Double,
    ): Result {
        require(dt > 0.0) { "dt must be positive" }
        val mass = effectiveMassKg.coerceAtLeast(1.0e-3)
        val maxStiffness = 4.0 * mass / (dt * dt)
        val stiffness = min(stiffnessNPerM, maxStiffness * 0.9)
        val normalized = stiffness * dt * dt / mass
        // Never sit exactly on the bound: a value equal to the limit makes the
        // solver's `limitIfNeeded` path drop the spring row entirely.
        // Explicit force integration updates the body velocity as
        //   v' = v * (1 - c*dt/m)
        // so the stability bound is c*dt/m < 2. The previous form used 1, which
        // threw away half of the usable damping and left the hull at zeta~0.24.
        val maxDamping = 2.0 * mass / dt * SAFETY
        val damping = min(dampingNsPerM, maxDamping)
        return Result(
            stiffnessNPerM = stiffness,
            dampingNsPerM = damping,
            maxStiffnessNPerM = maxStiffness,
            maxDampingNsPerM = maxDamping,
            limited = stiffness < stiffnessNPerM - 1.0e-6 || damping < dampingNsPerM - 1.0e-6,
        )
    }

    /** Reduced mass of two bodies joined by the axle. */
    fun effectiveMassKg(bodyA: Double, bodyB: Double): Double =
        if (bodyA <= 0.0 || bodyB <= 0.0) 0.0 else 1.0 / (1.0 / bodyA + 1.0 / bodyB)
}
