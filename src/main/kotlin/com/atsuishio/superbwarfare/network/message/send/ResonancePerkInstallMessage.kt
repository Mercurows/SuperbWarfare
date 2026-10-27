package com.atsuishio.superbwarfare.network.message.send

import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.atsuishio.superbwarfare.ksp.annotation.RegisterPacket
import com.atsuishio.superbwarfare.network.PayloadContext
import com.atsuishio.superbwarfare.network.ServerPacketPayload
import com.atsuishio.superbwarfare.perk.Perk
import kotlinx.serialization.Serializable
import net.minecraft.core.BlockPos
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

/** One atomic installation: validate the table, weapon identity, compatibility and all costs first. */
@RegisterPacket
@Serializable
data class ResonancePerkInstallMessage(
    val tablePos: Long,
    val inventorySlot: Int,
    val weaponUuid: String,
    val expectedPerks: String,
    val ammo: List<String>,
    val functional: List<String>,
    val damage: List<String>,
) : ServerPacketPayload() {
    override fun PayloadContext.handler() {
        val player = sender()
        val pos = BlockPos.of(tablePos)
        val level = player.level()
        if (!player.isAlive || player.isSpectator || !level.hasChunkAt(pos) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 64.0
            || !level.getBlockState(pos).`is`(ModBlocks.REFORGING_TABLE.get())
            || !level.getBlockState(pos.below()).`is`(ModBlocks.RESONANCE_BASE.get())) {
            return
        }
        if (inventorySlot !in player.inventory.items.indices) return
        val stack = player.inventory.getItem(inventorySlot)
        if (stack.isEmpty || stack.item !is GunItem) return
        val data = GunData.from(stack)
        if (weaponUuid.isBlank() || data.uuid?.toString() != weaponUuid || signature(data) != expectedPerks) {
            return
        }
        val requested = mapOf(Perk.Type.AMMO to ammo, Perk.Type.FUNCTIONAL to functional, Perk.Type.DAMAGE to damage)
        val installed = Perk.Type.entries.associateWith { data.perk.getInstances(it) }
        val selections = mutableMapOf<Perk.Type, List<Perk>>()
        val additions = mutableListOf<Perk>()
        val removals = mutableListOf<Perk>()
        for (type in Perk.Type.entries) {
            val names = requested.getValue(type)
            val current = installed.getValue(type).map { it.perk }
            val allowed = data.availablePerks().filter { it.type == type }
            val change = planChange(current.map { it.name }, names, allowed.map { it.name }.toSet())
                ?: return
            val byName = (current + allowed).associateBy { it.name }
            val next = names.map { byName.getValue(it) }
            selections[type] = next
            additions += change.added.map { byName.getValue(it) }
            removals += change.removed.map { byName.getValue(it) }
        }

        // Read every required item and build the result on a copy before touching the inventory.
        val costs = if (player.isCreative) emptyMap() else additions.groupingBy { it.getItem().get() }.eachCount()
        val refunds = if (player.isCreative) emptyList() else removals.map { ItemStack(it.getItem().get()) }
        val available = player.inventory.items.filter { !it.isEmpty }.groupingBy { it.item }
            .fold(0) { count, item -> count + item.count }
        if (costs.any { (item, count) -> (available[item] ?: 0) < count }) {
            return
        }
        val result = stack.copy()
        applySelection(GunData.from(result), selections)
        val remaining = costs.toMutableMap()
        for (itemStack in player.inventory.items) {
            val needed = remaining[itemStack.item] ?: continue
            val consumed = minOf(needed, itemStack.count)
            remaining[itemStack.item] = needed - consumed
            itemStack.shrink(consumed)
        }
        player.inventory.setItem(inventorySlot, result)
        for (refund in refunds) if (!player.addItem(refund)) player.drop(refund, false)
        player.inventory.setChanged()
        player.inventoryMenu.broadcastChanges()
        if (player.containerMenu !== player.inventoryMenu) player.containerMenu.broadcastChanges()
    }

    companion object {
        data class Change(val added: List<String>, val removed: List<String>)

        /** Pure planning step: invalid selections never reach inventory mutation. */
        fun planChange(current: List<String>, requested: List<String>, allowed: Set<String>): Change? {
            if (requested.size > 128 || requested.distinct().size != requested.size
                || requested.size > maxOf(current.size, allowed.size)
                || requested.any { it !in current && it !in allowed }) return null
            return Change(requested.filter { it !in current }, current.filter { it !in requested })
        }

        /** Ignore transient perk counters when checking whether the installed selection changed. */
        fun signature(data: GunData): String = Perk.Type.entries.joinToString(";") { type ->
            type.typeName + ":" + data.perk.getInstances(type).joinToString(",") { "${it.perk.name}@${it.level}" }
        }

        /** Used on a client preview copy and, after validation, on a server result copy. */
        fun applySelection(data: GunData, selection: Map<Perk.Type, List<Perk>>) {
            val root = data.perk()
            for (type in Perk.Type.entries) {
                val old = if (root.contains(type.typeName, Tag.TAG_LIST.toInt())) {
                    root.getList(type.typeName, Tag.TAG_COMPOUND.toInt()).filterIsInstance<CompoundTag>()
                } else listOf(root.getCompound(type.typeName))
                val byName = old.associateBy { it.getString("Name") }
                val next = ListTag()
                for (perk in selection[type].orEmpty()) {
                    next.add(byName[perk.name]?.copy() ?: CompoundTag().apply {
                        putString("Name", perk.name)
                        putShort("Level", 1)
                    })
                }
                if (next.isEmpty()) root.remove(type.typeName) else root.put(type.typeName, next)
            }
            data.invalidateProperties()
            data.save()
        }
    }
}
