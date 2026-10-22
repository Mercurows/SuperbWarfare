package com.atsuishio.superbwarfare.physics.tracked

import net.minecraft.world.phys.Vec3

/**
 * Inertia of a body from its mass and its *physical* shape.
 *
 * Only shapes the compiler actually builds are modelled, so a number here can
 * always be traced to an authored mass and a measured extent. Hull inertia is
 * not derivable from the mechanism definition and is deliberately absent.
 */
object TrackedMassProperties {

    /**
     * Uniform solid box about its own centre of mass.
     *
     * `I_x = m (a_y^2 + a_z^2) / 12`, and cyclically for the other axes, where
     * `a` are the full extents.
     */
    fun box(name: String, massKg: Double, halfExtents: Vec3): CompiledBodyMass {
        val a = halfExtents.x * 2.0
        val b = halfExtents.y * 2.0
        val c = halfExtents.z * 2.0
        val factor = massKg / 12.0
        return CompiledBodyMass(
            name = name,
            massKg = massKg,
            inertiaKgM2 = Vec3(
                factor * (b * b + c * c),
                factor * (a * a + c * c),
                factor * (a * a + b * b),
            ),
            basis = MassBasis.BOX,
        )
    }

    /**
     * Uniform solid cylinder whose axis is [axleDirection].
     *
     * `I_axis = m r^2 / 2` and `I_perp = m (3r^2 + h^2) / 12`. The result is
     * written in the chassis frame, so a lateral axle puts the axial inertia on
     * `x`.
     */
    fun cylinder(
        name: String,
        massKg: Double,
        radius: Double,
        halfWidth: Double,
        axleDirection: Vec3,
    ): CompiledBodyMass {
        val height = halfWidth * 2.0
        val axial = massKg * radius * radius * 0.5
        val perpendicular = massKg * (3.0 * radius * radius + height * height) / 12.0
        val unit = axleDirection.normalize()
        return CompiledBodyMass(
            name = name,
            massKg = massKg,
            inertiaKgM2 = Vec3(
                if (unit.x * unit.x > 0.5) axial else perpendicular,
                if (unit.y * unit.y > 0.5) axial else perpendicular,
                if (unit.z * unit.z > 0.5) axial else perpendicular,
            ),
            basis = MassBasis.CYLINDER,
        )
    }
}
