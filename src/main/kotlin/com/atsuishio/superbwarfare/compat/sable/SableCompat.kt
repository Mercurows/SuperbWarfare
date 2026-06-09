package com.atsuishio.superbwarfare.compat.sable

/**
 * Compatibility layer for Sable (Create: Aeronautics physics engine).
 *
 * When Sable is present, VehicleEntity delegates to Entity.move() (via super.move())
 * instead of using its custom vMove(). This allows Sable's Entity.move() mixin wrap
 * (sable$moveInject) to inject sub-level collision detection — the same mechanism
 * that supports LivingEntity on physicalized contraption surfaces.
 */
object SableCompat {

    private var available: Boolean? = null

    @JvmStatic
    fun isSableAvailable(): Boolean {
        if (available == null) {
            available = try {
                Class.forName("dev.ryanhcode.sable.Sable")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
        return available!!
    }
}
