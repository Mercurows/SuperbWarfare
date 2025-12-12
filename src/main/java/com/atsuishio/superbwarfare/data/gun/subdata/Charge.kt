package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.Starter
import com.atsuishio.superbwarfare.data.gun.value.Timer

class Charge(data: GunData) {

    @JvmField
    val timer = Timer(data.data(), "Charge")

    @JvmField
    val starter = Starter(data.data(), "Charge")

    fun time(): Int {
        return timer.get()
    }
}
