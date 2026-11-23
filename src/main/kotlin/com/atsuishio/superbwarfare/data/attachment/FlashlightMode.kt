package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.gun.GunData

enum class FlashlightMode {
    OFF,
    ON,
    STROBE,
    LASER;

    fun next(): FlashlightMode = values()[(ordinal + 1) % values().size]

    companion object {
        fun byOrdinal(value: Int): FlashlightMode = values().getOrElse(value) { OFF }
    }
}

object FlashlightState {
    private const val MODE_KEY = "FlashlightMode"
    private const val LEGACY_ON_KEY = "FlashlightOn"

    fun availableModes(gun: GunData): List<FlashlightMode> {
        val hasFlashlight = AttachmentDefinition.findFlashlights(gun).isNotEmpty()
        val hasLaser = AttachmentDefinition.findLaserEmitters(gun).isNotEmpty()
        return when {
            // 双模式（手电 + 激光）：激光 → 常亮 → 爆闪 → 关闭 → 激光
            hasFlashlight && hasLaser -> listOf(
                FlashlightMode.LASER,
                FlashlightMode.ON,
                FlashlightMode.STROBE,
                FlashlightMode.OFF,
            )
            hasLaser -> listOf(FlashlightMode.OFF, FlashlightMode.LASER)
            hasFlashlight -> listOf(FlashlightMode.OFF, FlashlightMode.ON, FlashlightMode.STROBE)
            else -> emptyList()
        }
    }

    fun modeOf(gun: GunData): FlashlightMode {
        val data = gun.data()
        if (data.contains(MODE_KEY)) return FlashlightMode.byOrdinal(data.getInt(MODE_KEY))
        if (data.getBoolean(LEGACY_ON_KEY)) return FlashlightMode.ON
        val hasLaser = AttachmentDefinition.findLaserEmitters(gun).isNotEmpty()
        if (hasLaser) return FlashlightMode.LASER
        val hasFlashlight = AttachmentDefinition.findFlashlights(gun).isNotEmpty()
        return if (hasFlashlight) FlashlightMode.ON else FlashlightMode.OFF
    }

    fun setMode(gun: GunData, mode: FlashlightMode) {
        val data = gun.data()
        data.putInt(MODE_KEY, mode.ordinal)
        data.remove(LEGACY_ON_KEY)
        gun.invalidateProperties()
        gun.save()
    }

    fun next(gun: GunData): FlashlightMode {
        val modes = availableModes(gun)
        if (modes.isEmpty()) return FlashlightMode.OFF
        val current = modeOf(gun)
        val index = modes.indexOf(current)
        return if (index >= 0) modes[(index + 1) % modes.size] else modes.first()
    }

    fun cycle(gun: GunData): FlashlightMode {
        val next = next(gun)
        setMode(gun, next)
        return next
    }
}
