package com.atsuishio.superbwarfare.entity.vehicle.base

/** Pure rolling-resistance math for vehicles whose manual engine is not ready. */
object VehicleEngineCoasting {
    private const val DAMPING = 0.99

    data class Velocity(val x: Double, val y: Double, val z: Double)

    @JvmStatic
    fun coast(x: Double, y: Double, z: Double, onGround: Boolean): Velocity = Velocity(
        x = x * DAMPING,
        y = y * if (onGround) 1.0 else DAMPING,
        z = z * DAMPING
    )
}
