package com.atsuishio.superbwarfare.physics

/**
 * Registration of the conventional ground vehicles whose motion is owned by
 * Spark/Bullet.
 *
 * [contains] is registration only. Compilation is checked by
 * `VehiclePhysics.supportsPhysics`: a registered tracked id whose definition
 * does not compile stays out of the physics path rather than falling back to
 * wheeled suspension.
 *
 * Wheeled vehicles remain parked: re-enabling them belongs to the B0 baseline.
 */
internal object GroundVehicleAllowlist {
    /** Suspension model registered to one enabled ground vehicle. */
    enum class Model {
        WHEEL_JOINT,
        /** Dynamic track runs, wheel-end bodies and compiled suspension rows. */
        TRACK_RUN,
    }

    private val enabled = linkedMapOf<String, Model>(
        // S2 first positive vehicle. The tracked runtime is assembled from the
        // compiled definition, so this entry is only meaningful while that
        // definition compiles; a failure keeps the vehicle out of physics.
        "superbwarfare:ztz_99a" to Model.TRACK_RUN,
    )

    fun contains(id: String): Boolean = id in enabled

    /** Registered suspension model; unknown ids fall back to the wheeled model. */
    fun modelOf(id: String): Model = enabled[id] ?: Model.WHEEL_JOINT

    /** Stable snapshot for data-driven consumers (creative tab, containers). */
    fun enabledIds(): Set<String> = enabled.keys
}
