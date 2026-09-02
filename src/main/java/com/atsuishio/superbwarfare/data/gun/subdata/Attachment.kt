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
        gun.nbtVersion.invalidateStructural()
    }

    /**
     * Returns the registered attachment id installed in [type].
     */
    fun id(type: AttachmentType): ResourceLocation? {
        val tag = attachment.get(type.attachmentName) ?: return null
        return when (tag.id) {
            Tag.TAG_STRING -> ResourceLocation.tryParse(attachment.getString(type.attachmentName))
            Tag.TAG_COMPOUND -> ResourceLocation.tryParse(attachment.getCompound(type.attachmentName).getString("Id"))
            else -> null
        }
    }

    /**
     * Returns the persisted per-instance NBT tag for [type], if present.
     */
    fun getTag(type: AttachmentType): CompoundTag? {
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
        if (!attachment.contains(name, Tag.TAG_COMPOUND.toInt())) {
            attachment.put(name, CompoundTag())
        }
        return attachment.getCompound(name)
    }

    fun has(type: AttachmentType): Boolean = id(type) != null

    fun set(type: AttachmentType, id: ResourceLocation?) {
        if (id == null) {
            remove(type)
            return
        }
        if (id(type) == id) return

        val tag = CompoundTag().apply { putString("Id", id.toString()) }
        AttachmentDefinition.from(id)?.zoom?.let {
            tag.putDouble("Zoom", it.default)
        }

        attachment.put(type.attachmentName, tag)
        gun.nbtVersion.invalidateStructural()
    }

    fun remove(type: AttachmentType) {
        if (!attachment.contains(type.attachmentName)) return
        attachment.remove(type.attachmentName)
        gun.nbtVersion.invalidateStructural()
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
        gun.nbtVersion.invalidateStructural()
    }

    fun cycleZoom(type: AttachmentType, amount: Double): Double? {
        val id = id(type) ?: return null
        val definition = AttachmentDefinition.from(id) ?: return null
        val zoomConfig = definition.zoom ?: return null

        val current = getZoom(type) ?: zoomConfig.default
        val next = (current + amount * zoomConfig.step).coerceIn(zoomConfig.min, zoomConfig.max)
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
