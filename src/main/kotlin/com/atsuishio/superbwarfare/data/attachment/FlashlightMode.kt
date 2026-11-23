package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunState

enum class FlashlightMode {
    OFF,
    ON,
    STROBE;

    fun next(): FlashlightMode = entries[(ordinal + 1) % entries.size]

    companion object {
        fun byOrdinal(value: Int): FlashlightMode = entries.getOrElse(value) { OFF }
    }
}

object FlashlightState {
    fun modeOf(state: GunState): FlashlightMode = state.flashlightMode?.let(FlashlightMode::byOrdinal)
        ?: if (state.flashlightOn) FlashlightMode.ON else FlashlightMode.OFF

    fun cycle(gun: GunData) {
        gun.update { state ->
            state.copy(flashlightMode = modeOf(state).next().ordinal, flashlightOn = false)
        }
    }
}
