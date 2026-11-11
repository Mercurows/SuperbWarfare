package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.config.server.GunConfig
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.subdata.Attachment
import com.atsuishio.superbwarfare.data.gun.subweapon.SubWeaponRuntime
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.item.attachment.AttachmentProvider
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraftforge.registries.ForgeRegistries
import java.util.*

/** Shared preview/server validation. Preparation only mutates a private copy of the gun. */
object GunModificationService {
    private val slots get() = GunModificationCode.SLOTS.map { name -> AttachmentType.entries.first { it.attachmentName == name } }

    fun capture(data: GunData): GunModificationPreset {
        val attachments = linkedMapOf<String, List<GunModificationEntry>>()
        for (slot in slots) {
            val nodes = nodes(data, slot)
            if (nodes.isEmpty()) continue
            attachments[slot.attachmentName] = nodes.map { (id, tag) ->
                val definition = AttachmentDefinition.from(id) ?: error("missing", id.toString())
                val mode = tag.getInt("Mode")
                val zoom = tag.takeIf { it.contains("Zoom") }?.getDouble("Zoom")
                    ?.takeUnless { it == definition.scopeZoom(mode)?.default }
                GunModificationEntry(GunModificationCode.shortId(id.toString()),
                    data.attachment.getOffset(slot, tag), data.attachment.getRotation(slot, tag),
                    tag.takeIf { it.contains("LaserColor") }?.getInt("LaserColor")?.let {
                        if (it !in 0..0xFFFFFF) error("invalid")
                        it.toString(16).uppercase(Locale.ROOT).padStart(6, '0')
                    }, tag.getString(AttachmentDefinition.CURRENT_SKIN_ID), mode, zoom)
            }
        }
        val preset = GunModificationPreset(GunModificationCode.shortId(data.id), attachments)
        return preset.copy(minimumMode = minimumMode(data, preset))
    }

    fun fingerprint(data: GunData): String = GunModificationCode.encode(capture(data))

    private fun minimumMode(data: GunData, preset: GunModificationPreset): Int {
        var mode = 0
        val installed = mutableListOf<Pair<AttachmentType, AttachmentDefinition>>()
        for ((name, entries) in preset.attachments) {
            val slot = slots.first { it.attachmentName == name }
            val allowed = AvailableAttachments.resolve(data, slot).map { it.id }.toSet()
            for (entry in entries) {
                val id = ResourceLocation(GunModificationCode.fullId(entry.id))
                val definition = AttachmentDefinition.from(id) ?: continue
                if (id !in allowed || entry.rotation != 0.0 &&
                    !(slot == AttachmentType.MUZZLE && definition.canRotate) ||
                    entry.offset != 0.0 && slot !in normalOffsetSlots) mode = 2
                installed += slot to definition
            }
        }
        if (mode < 2 && installed.any { (slot, definition) -> installed.any { (other, otherDefinition) ->
                AttachmentSlots.conflicts(slot, definition, other, otherDefinition, data)
            } }) mode = 1
        return mode
    }

    data class Prepared(
        val preset: GunModificationPreset,
        val preview: GunData,
        val needed: Map<ResourceLocation, Int>,
        val returned: Map<ResourceLocation, Int>,
        val missing: Map<ResourceLocation, Int>,
    )

    fun prepare(data: GunData, preset: GunModificationPreset, player: Player): Prepared {
        if (!data.item.canEditAttachments(data)) error("unavailable")
        if (GunModificationCode.fullId(preset.gun) != data.id) error("gun", GunModificationCode.fullId(preset.gun))
        val preview = GunData.from(data.stack.copy())
        slots.forEach { preview.attachment.clear(it) }
        // Validate against the final assembly, with the old configuration completely removed.
        for (slot in slots) {
            for (entry in preset.attachments[slot.attachmentName].orEmpty()) {
                val id = ResourceLocation(GunModificationCode.fullId(entry.id))
                val definition = AttachmentDefinition.from(id) ?: error("missing", id.toString())
                val stacking = preview.attachment.acceptsStacked(slot)
                if (!preview.canInstall(slot, id) || slot == AttachmentType.SCOPE &&
                    preview.attachment.has(slot) && !stacking) error("incompatible", id.toString())
                preview.attachment.set(slot, id)
                val tag = if (slot == AttachmentType.SCOPE) preview.attachment.scopeStack().last().tag
                    else preview.attachment.getOrCreateTag(slot)
                writeSettings(tag, entry, definition)
            }
        }
        preview.invalidateProperties()
        if (!GunConfig.freeAttachmentMode) {
            val installed = preview.attachment.installed()
            for (entry in installed) for (other in installed) {
                if (AttachmentSlots.conflicts(entry.slot, entry.definition, other.slot, other.definition, preview))
                    error("incompatible", entry.id.toString())
            }
        }
        for (slot in slots) {
            val entries = preset.attachments[slot.attachmentName].orEmpty()
            for (entry in entries) {
                val id = ResourceLocation(GunModificationCode.fullId(entry.id))
                val definition = AttachmentDefinition.from(id)!!
                val canRotate = slot != AttachmentType.MAGAZINE && slot != AttachmentType.CHARM &&
                    (GunConfig.fullyFreeAttachmentMode || slot == AttachmentType.MUZZLE && definition.canRotate)
                if (entry.rotation != 0.0 && !canRotate || entry.offset != 0.0 && !AttachmentAdjustments.canOffset(slot))
                    error("adjustment", id.toString())
                if (entry.offset != 0.0 && entry.offset !in AttachmentAdjustments.offsetRange(preview, slot)) error("range", id.toString())
            }
        }
        val oldCounts = counts(capture(data))
        val newCounts = counts(preset)
        val needed = newCounts.mapValues { (id, count) -> (count - (oldCounts[id] ?: 0)).coerceAtLeast(0) }.filterValues { it > 0 }
        val returned = oldCounts.mapValues { (id, count) -> (count - (newCounts[id] ?: 0)).coerceAtLeast(0) }.filterValues { it > 0 }
        val inventory = inventory(player).filterNot { it.isEmpty }.groupingBy(::attachmentId).fold(0) { count, stack -> count + stack.count }
        val missing = if (GunConfig.isUnrestrictedModification(player)) emptyMap() else needed
            .mapValues { (id, count) -> (count - (inventory[id] ?: 0)).coerceAtLeast(0) }.filterValues { it > 0 }
        if (!GunConfig.isUnrestrictedModification(player)) returned.keys.forEach {
            if (itemFor(it).isEmpty) error("missing", it.toString())
        }
        return Prepared(preset, preview, needed, returned, missing)
    }

    /** Called on the server thread immediately after prepare; all checks precede inventory changes. */
    fun apply(player: ServerPlayer, data: GunData, prepared: Prepared) {
        if (prepared.missing.isNotEmpty()) error("items", prepared.missing.entries.joinToString { "${it.key} ×${it.value}" })
        // Materialize carrier roots before matching, so retained sub-weapons keep their live state.
        val subWeapons = SubWeaponRuntime.installed(data, false)
        val old = slots.flatMap { slot -> nodes(data, slot).map { (id, tag) -> Existing(slot, id, tag) } }.toMutableList()
        val targets = slots.associateWith { slot -> prepared.preset.attachments[slot.attachmentName].orEmpty().map { entry ->
            val id = ResourceLocation(GunModificationCode.fullId(entry.id))
            val match = old.firstOrNull { it.slot == slot && it.id == id } ?: old.firstOrNull { it.id == id }
            if (match != null) old.remove(match)
            Triple(entry, AttachmentDefinition.from(id)!!, match?.tag ?: CompoundTag().apply { putString("Id", id.toString()) })
        } }
        if (!GunConfig.isUnrestrictedModification(player)) {
            for ((id, count) in prepared.needed) {
                var remaining = count
                for (stack in inventory(player)) {
                    if (attachmentId(stack) != id) continue
                    val take = minOf(remaining, stack.count)
                    stack.shrink(take)
                    remaining -= take
                    if (remaining == 0) break
                }
            }
        }
        // Removed sub-weapons return loaded rounds; reused ones retain their rounds in place.
        for (instance in subWeapons) {
            if (old.any { it.slot == instance.slot && it.id == instance.attachmentId }) instance.data.withdrawAmmo(player)
        }
        if (data.attachment.id(AttachmentType.MAGAZINE) != prepared.preview.attachment.id(AttachmentType.MAGAZINE)) data.withdrawAmmo(player)
        for (slot in slots) {
            val entries = targets.getValue(slot)
            for ((entry, definition, tag) in entries) {
                tag.remove(Attachment.NESTED_KEY)
                writeSettings(tag, entry, definition)
            }
            entries.zipWithNext { lower, upper -> lower.third.put(Attachment.NESTED_KEY, upper.third) }
            if (entries.isEmpty()) data.attachment.clear(slot) else data.attachment.setTag(slot, entries.first().third)
        }
        data.invalidateProperties()
        data.save()
        if (!GunConfig.isUnrestrictedModification(player)) for ((id, count) in prepared.returned) {
            repeat(count) {
                val stack = itemFor(id)
                if (!player.addItem(stack)) player.drop(stack, false)
            }
        }
    }

    private data class Existing(val slot: AttachmentType, val id: ResourceLocation, val tag: CompoundTag)

    private fun writeSettings(tag: CompoundTag, entry: GunModificationEntry, definition: AttachmentDefinition) {
        if (entry.color != null && definition.laser == null) error("adjustment", entry.id)
        if (entry.skin.isNotEmpty() && definition.skinOrNull(entry.skin) == null) error("skin", entry.skin)
        if (entry.scopeMode !in 0 until (definition.scopeInfo?.modeCount() ?: 1)) error("adjustment", entry.id)
        val zoom = definition.scopeZoom(entry.scopeMode)
        if (entry.zoom != null && (zoom == null || entry.zoom !in zoom.min..zoom.max)) error("range", entry.id)
        for (key in listOf("Offset", "Rotation", "LaserColor", AttachmentDefinition.CURRENT_SKIN_ID, "Mode", "Zoom")) tag.remove(key)
        if (entry.offset != 0.0) tag.putDouble("Offset", entry.offset)
        if (entry.rotation != 0.0) tag.putDouble("Rotation", entry.rotation)
        entry.color?.let { tag.putInt("LaserColor", it.toInt(16)) }
        if (entry.skin.isNotEmpty()) tag.putString(AttachmentDefinition.CURRENT_SKIN_ID, entry.skin)
        if (entry.scopeMode != 0) tag.putInt("Mode", entry.scopeMode)
        (entry.zoom ?: zoom?.default)?.let { tag.putDouble("Zoom", it) }
    }

    private fun nodes(data: GunData, slot: AttachmentType): List<Pair<ResourceLocation, CompoundTag>> {
        if (slot == AttachmentType.SCOPE) return data.attachment.scopeStack().map { it.id to it.tag }
        val id = data.attachment.id(slot) ?: return emptyList()
        return listOf(id to (data.attachment.getTagIfCompound(slot) ?: CompoundTag().apply { putString("Id", id.toString()) }))
    }

    private fun counts(preset: GunModificationPreset) = preset.attachments.values.flatten()
        .groupingBy { ResourceLocation(GunModificationCode.fullId(it.id)) }.eachCount()

    private fun inventory(player: Player) = player.inventory.items + player.inventory.offhand
    private fun attachmentId(stack: ItemStack): ResourceLocation? = if (stack.isEmpty) null else
        (stack.item as? AttachmentProvider)?.attachmentId?.let(ResourceLocation::tryParse)

    fun itemFor(id: ResourceLocation): ItemStack = ForgeRegistries.ITEMS.values.firstOrNull {
        (it as? AttachmentProvider)?.attachmentId?.let(ResourceLocation::tryParse) == id
    }?.let(::ItemStack) ?: ItemStack.EMPTY

    private fun error(reason: String, vararg details: String): Nothing = throw GunModificationException(reason, *details)
    private val normalOffsetSlots = setOf(AttachmentType.SCOPE, AttachmentType.GRIP, AttachmentType.UPPER_RAIL,
        AttachmentType.LOWER_RAIL, AttachmentType.LEFT_RAIL, AttachmentType.RIGHT_RAIL)
}
