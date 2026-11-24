package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType

/**
 * 单件照明/指示配件的档位。
 *
 * ordinal 会写进**配件自己的槽位 NBT**，只能追加、不能重排。
 */
enum class FlashlightMode {
    OFF,
    ON,
    STROBE,
    LASER;

    companion object {
        fun byOrdinal(value: Int): FlashlightMode = values().getOrElse(value) { OFF }
    }
}

/**
 * 每件配件独立的激光/手电档位（存在 `Attachments.<Slot>.LightMode` 里，而不是枪 NBT）。
 *
 * 档位循环按配件能力决定：
 * - 手电 + 激光（X400 二合一）：`LASER → ON → STROBE → OFF → LASER`
 * - 纯激光（PEQ-15 / 手枪激光）：`LASER ↔ OFF`
 * - 纯手电（垂直握把）：`ON → STROBE → OFF → ON`
 *
 * 槽位 NBT 没有 `LightMode` 时使用该配件的默认档位（有激光 → `LASER`，纯手电 → `ON`）。
 * 枪级旧键 `FlashlightMode` / `FlashlightOn` 不再读取。
 */
object AttachmentLightState {
    /** 槽位 NBT 里的键名（每件配件独立）。 */
    const val MODE_KEY = "LightMode"

    fun hasEmitter(definition: AttachmentDefinition?): Boolean =
        definition != null && (definition.laser != null || definition.flashlight != null)

    /** 该配件可用的档位循环，顺序即按键/UI 顺序。 */
    fun cycleFor(definition: AttachmentDefinition?): List<FlashlightMode> {
        val laser = definition?.laser != null
        val flashlight = definition?.flashlight != null
        return when {
            laser && flashlight -> listOf(
                FlashlightMode.LASER,
                FlashlightMode.ON,
                FlashlightMode.STROBE,
                FlashlightMode.OFF,
            )
            laser -> listOf(FlashlightMode.LASER, FlashlightMode.OFF)
            flashlight -> listOf(FlashlightMode.ON, FlashlightMode.STROBE, FlashlightMode.OFF)
            else -> emptyList()
        }
    }

    fun defaultMode(definition: AttachmentDefinition?): FlashlightMode =
        cycleFor(definition).firstOrNull() ?: FlashlightMode.OFF

    fun definitionOf(gun: GunData, slot: AttachmentType): AttachmentDefinition? =
        gun.attachment.id(slot)?.let { AttachmentDefinition.from(it) }

    fun cycleFor(gun: GunData, slot: AttachmentType): List<FlashlightMode> =
        cycleFor(definitionOf(gun, slot))

    /** 该枪上所有会发光/出激光的槽位（按 [AttachmentSlots.ALL] 顺序）。 */
    fun emitterSlots(gun: GunData): List<AttachmentType> = AttachmentSlots.ALL.mapNotNull { slot ->
        if (hasEmitter(definitionOf(gun, slot.type))) slot.type else null
    }

    private fun normalize(mode: FlashlightMode, cycle: List<FlashlightMode>): FlashlightMode {
        if (cycle.isEmpty()) return FlashlightMode.OFF
        if (mode in cycle) return mode
        if (mode == FlashlightMode.OFF && FlashlightMode.OFF in cycle) return FlashlightMode.OFF
        if (mode == FlashlightMode.LASER && FlashlightMode.LASER in cycle) return FlashlightMode.LASER
        if (FlashlightMode.ON in cycle) return FlashlightMode.ON
        return cycle.first()
    }

    fun modeOf(gun: GunData, slot: AttachmentType): FlashlightMode {
        val definition = definitionOf(gun, slot)
        val cycle = cycleFor(definition)
        if (cycle.isEmpty()) return FlashlightMode.OFF

        val tag = gun.attachment.getTagIfCompound(slot)
        if (tag != null && tag.contains(MODE_KEY)) {
            return normalize(FlashlightMode.byOrdinal(tag.getInt(MODE_KEY)), cycle)
        }
        return normalize(defaultMode(definition), cycle)
    }

    /** 写单件配件的档位；批量写时由调用方统一 invalidate/save。 */
    private fun writeMode(gun: GunData, slot: AttachmentType, mode: FlashlightMode) {
        gun.attachment.getOrCreateTag(slot).putInt(MODE_KEY, mode.ordinal)
    }

    fun setMode(gun: GunData, slot: AttachmentType, mode: FlashlightMode) {
        val definition = definitionOf(gun, slot) ?: return
        if (!hasEmitter(definition)) return
        val normalized = normalize(mode, cycleFor(definition))
        // 值没变且槽位已有显式档位时不写 NBT，避免无意义的 revision 增长/同步。
        if (modeOf(gun, slot) == normalized &&
            gun.attachment.getTagIfCompound(slot)?.contains(MODE_KEY) == true
        ) return
        writeMode(gun, slot, normalized)
        gun.invalidateProperties()
        gun.save()
    }

    fun next(gun: GunData, slot: AttachmentType): FlashlightMode {
        val cycle = cycleFor(gun, slot)
        if (cycle.isEmpty()) return FlashlightMode.OFF
        val current = modeOf(gun, slot)
        val index = cycle.indexOf(current)
        return if (index >= 0) cycle[(index + 1) % cycle.size] else cycle.first()
    }

    fun cycle(gun: GunData, slot: AttachmentType): FlashlightMode {
        val next = next(gun, slot)
        setMode(gun, slot, next)
        return next
    }

    /**
     * 短按：所有发光配件各自切到下一档。
     * @return 每件配件切换后的档位；没有发光配件时为空。
     */
    fun cycleAll(gun: GunData): List<Pair<AttachmentType, FlashlightMode>> {
        val result = emitterSlots(gun).mapNotNull { slot ->
            val cycle = cycleFor(gun, slot)
            if (cycle.isEmpty()) return@mapNotNull null
            slot to next(gun, slot)
        }
        if (result.isEmpty()) return emptyList()
        result.forEach { (slot, mode) -> writeMode(gun, slot, mode) }
        gun.invalidateProperties()
        gun.save()
        return result
    }

    /**
     * 长按：所有发光配件全部关闭。
     * @return 被关闭的配件数量；本来就已经全关时返回 0（不写 NBT / 不 bump revision）。
     */
    fun turnOffAll(gun: GunData): Int {
        val slots = emitterSlots(gun)
        if (slots.isEmpty()) return 0
        val changed = slots.count { modeOf(gun, it) != FlashlightMode.OFF }
        if (changed == 0) return 0
        slots.forEach { slot -> writeMode(gun, slot, FlashlightMode.OFF) }
        gun.invalidateProperties()
        gun.save()
        return changed
    }
}
