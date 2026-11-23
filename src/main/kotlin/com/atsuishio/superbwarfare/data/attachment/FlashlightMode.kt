package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.gun.GunData

enum class FlashlightMode {
    OFF,
    ON,
    STROBE;

    fun next(): FlashlightMode = values()[(ordinal + 1) % values().size]

    companion object {
        fun byOrdinal(value: Int): FlashlightMode = values().getOrElse(value) { OFF }
    }
}

object FlashlightState {
    private const val MODE_KEY = "FlashlightMode"
    private const val LEGACY_ON_KEY = "FlashlightOn"

    fun modeOf(gun: GunData): FlashlightMode {
        val data = gun.data()
        if (data.contains(MODE_KEY)) return FlashlightMode.byOrdinal(data.getInt(MODE_KEY))
        return if (data.getBoolean(LEGACY_ON_KEY)) FlashlightMode.ON else FlashlightMode.OFF
    }

    fun setMode(gun: GunData, mode: FlashlightMode) {
        val data = gun.data()
        data.putInt(MODE_KEY, mode.ordinal)
        data.remove(LEGACY_ON_KEY)
        gun.save()
    }

    fun cycle(gun: GunData): FlashlightMode {
        val next = modeOf(gun).next()
        setMode(gun, next)
        return next
    }
}
