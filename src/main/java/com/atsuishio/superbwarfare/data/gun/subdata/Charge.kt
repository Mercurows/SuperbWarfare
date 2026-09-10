package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.StateStarter
import com.atsuishio.superbwarfare.data.gun.value.StateTimer

/**
 * Charge-fire state, backed by [com.atsuishio.superbwarfare.data.gun.GunState] instead of the gun tag.
 */
class Charge(gun: GunData) {

    @JvmField
    val timer = StateTimer(
        gun, { it.chargeTime }, { state, value -> state.copy(chargeTime = value) }, "Charge"
    )

    @JvmField
    val starter = StateStarter(
        gun, { it.startCharge }, { state, value -> state.copy(startCharge = value) }, "Charge"
    )

    fun time(): Int {
        return timer.get()
    }
}
