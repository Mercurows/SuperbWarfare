package com.atsuishio.superbwarfare.data.gun.subdata

import com.atsuishio.superbwarfare.data.attachment.AttachmentDefinition
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation

/**
 * Manages attachment slot data for a single [GunData] instance.
 *
 * Every write through [set] increments structural version so that
 * PMC calculations are invalidated and rebuilt lazily on demand.
 *
 * @param gun the owning [GunData] instance.
 */
class Attachment(private val gun: GunData) {

    private val attachment = gun.attachment()

    /**
     * Legacy integer slot accessor kept for existing renderers and item code.
     *
     * @param type the attachment slot to query.
     * @return slot index, or 0 if empty.
     */
    fun get(type: AttachmentType): Int = attachment.getInt(type.attachmentName)

    /**
     * Legacy integer writer kept for code that still uses old attachment ids.
     *
     * @param type slot to modify.
     * @param value legacy slot index.
     */
    fun set(type: AttachmentType, value: Int) {
        if (attachment.getInt(type.attachmentName) == value) return  // no-op: unchanged
        attachment.putInt(type.attachmentName, value)
        gun.invalidateProperties()
    }

    /**
     * Returns the registered attachment id installed in [type].
     */
    fun id(type: AttachmentType): ResourceLocation? {
        val tag = attachment.get(type.attachmentName) ?: return null
        return when (tag.id) {
            Tag.TAG_STRING -> ResourceLocation.tryParse(attachment.getString(type.attachmentName))
            Tag.TAG_COMPOUND -> ResourceLocation.tryParse(
                attachment.getCompound(type.attachmentName).readId()
            )

            else -> null
        }
    }

    /**
     * Returns the persisted per-instance NBT tag for [type], if present.
     */
    private fun getTag(type: AttachmentType): CompoundTag? {
        val tag = attachment.get(type.attachmentName) ?: return null
        return when (tag.id) {
            Tag.TAG_COMPOUND -> attachment.getCompound(type.attachmentName)
            Tag.TAG_STRING -> CompoundTag().apply {
                putString("Id", attachment.getString(type.attachmentName))
            }

            else -> null
        }
    }

    fun getOrCreateTag(type: AttachmentType): CompoundTag {
        val name = type.attachmentName
        val current = attachment.get(name)
        val tag = when (current?.id) {
            Tag.TAG_COMPOUND -> attachment.getCompound(name)
            Tag.TAG_STRING -> CompoundTag().apply {
                putString("Id", attachment.getString(name))
            }

            else -> CompoundTag()
        }
        attachment.put(name, tag)
        return tag
    }

    fun has(type: AttachmentType): Boolean = id(type) != null

    fun getRotation(type: AttachmentType): Double {
        val tag = getTag(type) ?: return 0.0
        if (!tag.contains("Rotation")) return 0.0
        return tag.getDouble("Rotation").coerceIn(0.0, 360.0)
    }

    fun setRotation(type: AttachmentType, rotation: Double) {
        if (!has(type)) return

        val value = rotation.coerceIn(0.0, 360.0)
        if (getRotation(type) == value) return

        getOrCreateTag(type).putDouble("Rotation", value)
        gun.invalidateProperties()
    }

    fun getOffset(type: AttachmentType): Double {
        val tag = getTag(type) ?: return 0.0
        if (!tag.contains("Offset")) return 0.0
        return tag.getDouble("Offset")
    }

    fun setOffset(type: AttachmentType, offset: Double) {
        if (!has(type)) return
        if (getOffset(type) == offset) return

        getOrCreateTag(type).putDouble("Offset", offset)
        gun.invalidateProperties()
    }

    fun set(type: AttachmentType, id: ResourceLocation?) {
        if (id == null) {
            remove(type)
            return
        }
        if (id(type) == id) return

        val tag = CompoundTag().apply { putString("Id", id.toString()) }
        AttachmentDefinition.from(id)?.let {
            it.scopeZoom(0)?.let { zoom -> tag.putDouble("Zoom", zoom.default) }
        }

        attachment.put(type.attachmentName, tag)
        gun.invalidateProperties()
    }

    fun remove(type: AttachmentType) {
        if (!attachment.contains(type.attachmentName)) return
        attachment.remove(type.attachmentName)
        gun.invalidateProperties()
    }

    fun cycle(type: AttachmentType, add: Boolean): Boolean {
        val allowed = gun.availableAttachments(type)
        if (allowed.isEmpty()) return false

        val optionCount = allowed.size + 1
        val currentIndex = allowed.indexOf(id(type)) + 1
        val nextIndex = if (add) {
            (currentIndex + 1) % optionCount
        } else {
            (currentIndex - 1 + optionCount) % optionCount
        }

        if (nextIndex == 0) {
            set(type, null)
        } else {
            set(type, allowed[nextIndex - 1])
        }
        return true
    }

    fun getZoom(type: AttachmentType): Double? {
        val tag = getTag(type) ?: return null
        if (!tag.contains("Zoom")) return null
        return tag.getDouble("Zoom")
    }

    fun setZoom(type: AttachmentType, zoom: Double) {
        getOrCreateTag(type).putDouble("Zoom", zoom)
        gun.invalidateProperties()
    }

    fun scopeMode(type: AttachmentType): Int {
        val tag = getTag(type) ?: return 0
        return if (tag.contains("Mode")) tag.getInt("Mode").coerceAtLeast(0) else 0
    }

    fun setScopeMode(type: AttachmentType, mode: Int) {
        getOrCreateTag(type).putInt("Mode", mode.coerceAtLeast(0))
        gun.invalidateProperties()
    }

    fun cycleScopeMode(type: AttachmentType, scroll: Double): Int {
        val id = id(type) ?: return 0
        val definition = AttachmentDefinition.from(id) ?: return 0
        if (!definition.supportsScopeSwitching()) return scopeMode(type)

        val count = definition.scopeInfo?.modeCount() ?: 1
        val current = scopeMode(type)
        val direction = if (scroll >= 0) 1 else -1
        val next = ((current + direction) % count + count) % count

        val tag = getOrCreateTag(type)
        tag.putInt("Mode", next)
        definition.scopeZoom(next)?.let { tag.putDouble("Zoom", it.default) }
        gun.invalidateProperties()
        return next
    }

    /**
     * Advances the installed scope's zoom by [amount] scroll notches.
     *
     * The step is proportional to the current zoom, so zooming is fine-grained at low
     * magnification and accelerates as magnification increases.
     *
     * @param type the attachment slot to adjust.
     * @param amount scroll direction and count (typically ±1 per notch).
     * @return the new zoom value, or null when the scope has no configurable zoom.
     */
    fun cycleZoom(type: AttachmentType, amount: Double): Double? {
        val id = id(type) ?: return null
        val definition = AttachmentDefinition.from(id) ?: return null
        val zoomConfig = definition.scopeZoom(scopeMode(type)) ?: return null

        val current = getZoom(type) ?: zoomConfig.default
        val next = (current * (1.0 + amount * zoomConfig.step)).coerceIn(zoomConfig.min, zoomConfig.max)
        setZoom(type, next)
        return next
    }

    fun installed(): List<AttachmentInstance> {
        val result = mutableListOf<AttachmentInstance>()
        for (type in AttachmentType.entries) {
            val id = id(type) ?: continue
            val definition = AttachmentDefinition.from(id) ?: continue
            if (definition.slot != type) continue
            val tag = getTag(type) ?: continue
            result += AttachmentInstance(type, id, tag, definition)
        }
        return result
    }
}

data class AttachmentInstance(
    val slot: AttachmentType,
    val id: ResourceLocation,
    val tag: CompoundTag,
    val definition: AttachmentDefinition,
)

private fun CompoundTag.readId(): String {
    val id = getString("Id")
    return id.ifBlank { getString("Name") }
}
