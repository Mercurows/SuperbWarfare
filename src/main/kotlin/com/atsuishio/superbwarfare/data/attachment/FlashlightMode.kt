package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.GunState
import com.atsuishio.superbwarfare.data.gun.subdata.AttachmentInstance

enum class FlashlightMode {
    OFF,
    ON,
    STROBE,
    LASER;

    val emitsLight get() = this == ON || this == STROBE

    companion object {
        fun byOrdinal(value: Int): FlashlightMode = entries.getOrElse(value) { OFF }
    }
}

object FlashlightState {
    private val laserModes = listOf(FlashlightMode.LASER, FlashlightMode.OFF)
    private val flashlightModes = listOf(FlashlightMode.ON, FlashlightMode.STROBE, FlashlightMode.OFF)
    private val combinedModes = listOf(FlashlightMode.LASER) + flashlightModes

    private fun availableModes(installed: List<AttachmentInstance>): List<FlashlightMode> {
        val hasLaser = installed.any { it.definition.laser != null }
        val hasFlashlight = installed.any { it.definition.flashlight != null }
        return when {
            hasLaser && hasFlashlight -> combinedModes
            hasLaser -> laserModes
            hasFlashlight -> flashlightModes
            else -> emptyList()
        }
    }

    fun modeOf(gun: GunData, installed: List<AttachmentInstance> = gun.attachment.installed()): FlashlightMode =
        modeOf(gun.state, availableModes(installed))

    private fun modeOf(state: GunState, modes: List<FlashlightMode>): FlashlightMode {
        val saved = state.flashlightMode?.let(FlashlightMode::byOrdinal)
            ?: FlashlightMode.ON.takeIf { state.flashlightOn && it in modes }
        return if (saved == null) modes.firstOrNull() ?: FlashlightMode.OFF
        else saved.takeIf { it in modes } ?: FlashlightMode.OFF
    }

    fun next(gun: GunData): FlashlightMode? {
        val modes = availableModes(gun.attachment.installed())
        if (modes.isEmpty()) return null
        return modes[(modes.indexOf(modeOf(gun.state, modes)) + 1) % modes.size]
    }

    fun cycle(gun: GunData) {
        val next = next(gun) ?: return
        gun.update { state ->
            state.copy(flashlightMode = next.ordinal, flashlightOn = false)
        }
    }
}
