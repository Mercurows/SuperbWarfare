package com.atsuishio.superbwarfare.physics

/**
 * Registration of the conventional ground vehicles whose motion is owned by
 * Spark/Bullet.
 *
 * D1 removed the probe-based tracked suspension and left no approved tracked
 * runtime, so the list is intentionally empty. Tracked vehicles remain
 * registered as entities and can still be spawned by commands or old saves,
 * but they are outside the conventional physics path and do not appear in the
 * creative vehicle tab or container loot.
 *
 * Wheeled vehicles are also parked here: re-enabling them belongs to the B0
 * baseline stage, not to the legacy cleanup.
 *
 * Re-enable a vehicle only after its stage evidence is recorded.
 */
internal object GroundVehicleAllowlist {
    /** Suspension model registered to one enabled ground vehicle. */
    enum class Model { WHEEL_JOINT }

    private val enabled = linkedMapOf<String, Model>()

    fun contains(id: String): Boolean = id in enabled

    /** Registered suspension model; unknown ids fall back to the wheeled model. */
    fun modelOf(id: String): Model = enabled[id] ?: Model.WHEEL_JOINT

    /** Stable snapshot for data-driven consumers (creative tab, containers). */
    fun enabledIds(): Set<String> = enabled.keys
}
