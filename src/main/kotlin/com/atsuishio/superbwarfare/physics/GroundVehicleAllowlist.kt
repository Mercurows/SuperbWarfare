package com.atsuishio.superbwarfare.physics

/**
 * Registration of the conventional ground vehicles whose motion is owned by
 * Spark/Bullet.
 *
 * The probe-based tracked suspension is the model under acceptance, so the
 * enabled set contains exactly the eight tracked vehicles. Wheeled vehicles
 * keep their authored vehicle data (wheel stations, physics settings), but
 * their joint-wheel model is parked until the tracked model is accepted; the
 * creative vehicle containers and the container loot pools filter through
 * [contains], so a parked vehicle is invisible in the inventory and cannot be
 * obtained in the meantime. Re-enable a vehicle by registering its id here.
 */
internal object GroundVehicleAllowlist {
    /** Suspension model registered to one enabled ground vehicle. */
    enum class Model { WHEEL_JOINT, TRACK_PROBE }

    private val enabled = linkedMapOf(
        "superbwarfare:bmp_2" to Model.TRACK_PROBE,
        "superbwarfare:bradley" to Model.TRACK_PROBE,
        "superbwarfare:m_1a_2" to Model.TRACK_PROBE,
        "superbwarfare:plz_05" to Model.TRACK_PROBE,
        "superbwarfare:prism_tank" to Model.TRACK_PROBE,
        "superbwarfare:t_90a" to Model.TRACK_PROBE,
        "superbwarfare:yx_100" to Model.TRACK_PROBE,
        "superbwarfare:ztz_99a" to Model.TRACK_PROBE,
    )

    fun contains(id: String): Boolean = id in enabled

    /** Registered suspension model; unknown ids fall back to the wheeled model. */
    fun modelOf(id: String): Model = enabled[id] ?: Model.WHEEL_JOINT

    /** Stable snapshot for data-driven consumers (creative tab, containers). */
    fun enabledIds(): Set<String> = enabled.keys
}
