package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModItems
import com.atsuishio.superbwarfare.inventory.menu.WeaponRepairStationMenu
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.items.wrapper.SidedInvWrapper

class WeaponRepairStationBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.WEAPON_REPAIR_STATION.get(), pos, state), WorldlyContainer, MenuProvider {
    private var items = NonNullList.withSize(2, ItemStack.EMPTY)
    var energy = 0
        private set
    var progress = 0
        private set
    var enabled = true
        private set
    private var itemHandlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH)

    // Vanilla data slots transmit signed shorts. Split each field, including configurable values.
    val data = object : ContainerData {
        override fun get(index: Int): Int {
            val value = when (index / 2) {
                0 -> energy
                1 -> capacity
                2 -> energyPerDurability
                3 -> repairInterval
                4 -> progress
                5 -> if (enabled) 1 else 0
                6 -> energyPerMaterial
                else -> 0
            }
            return (value ushr ((index % 2) * 16)) and 65535
        }
        override fun set(index: Int, value: Int) = Unit // Server authority; the client owns a SimpleContainerData.
        override fun getCount() = WeaponRepairStationMenu.DATA_COUNT
    }

    private fun tickRepair() {
        var changed = false
        val clamped = energy.coerceIn(0, capacity)
        if (clamped != energy) { energy = clamped; changed = true }
        val material = items[MATERIAL_SLOT]
        val gain = minOf(energyPerMaterial, capacity)
        // Never discard part of a kit's energy when storage is nearly full.
        if (isMaterial(material) && capacity - energy >= gain) {
            material.shrink(1)
            energy += gain
            changed = true
        }
        val target = items[REPAIR_SLOT]
        if (!canRepair(target)) {
            if (progress != 0) { progress = 0; changed = true }
        } else if (enabled && energy >= energyPerDurability) {
            progress++
            changed = true
            if (progress >= repairInterval) {
                target.damageValue = (target.damageValue - 1).coerceAtLeast(0)
                energy -= energyPerDurability
                progress = 0
            }
        }
        if (changed) setChanged()
    }

    fun toggleEnabled() { enabled = !enabled; setChanged() }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        ContainerHelper.saveAllItems(tag, items)
        tag.putInt("RepairEnergy", energy)
        tag.putInt("RepairProgress", progress)
        tag.putBoolean("RepairEnabled", enabled)
    }
    override fun load(tag: CompoundTag) {
        super.load(tag)
        items = NonNullList.withSize(2, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(tag, items)
        energy = tag.getInt("RepairEnergy").coerceAtLeast(0)
        progress = tag.getInt("RepairProgress").coerceAtLeast(0)
        enabled = !tag.contains("RepairEnabled") || tag.getBoolean("RepairEnabled")
    }

    override fun getContainerSize() = 2
    override fun isEmpty() = items.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = items[slot]
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(items, slot, amount).also {
        if (!it.isEmpty) { if (slot == REPAIR_SLOT) progress = 0; setChanged() }
    }
    override fun removeItemNoUpdate(slot: Int): ItemStack = ContainerHelper.takeItem(items, slot).also {
        if (slot == REPAIR_SLOT) progress = 0
    }
    override fun setItem(slot: Int, stack: ItemStack) {
        items[slot] = stack
        stack.count = minOf(stack.count, if (slot == REPAIR_SLOT) 1 else maxStackSize)
        if (slot == REPAIR_SLOT) progress = 0
        setChanged()
    }
    override fun clearContent() { items.clear(); progress = 0; setChanged() }
    override fun stillValid(player: Player) = Container.stillValidBlockEntity(this, player)
    override fun canPlaceItem(slot: Int, stack: ItemStack) = if (slot == MATERIAL_SLOT) isMaterial(stack) else isRepairTarget(stack)
    override fun getSlotsForFace(side: Direction): IntArray = if (side == Direction.UP || side == Direction.DOWN) intArrayOf(REPAIR_SLOT) else intArrayOf(MATERIAL_SLOT)
    override fun canPlaceItemThroughFace(slot: Int, stack: ItemStack, side: Direction?) = side != Direction.DOWN && canPlaceItem(slot, stack)
    override fun canTakeItemThroughFace(slot: Int, stack: ItemStack, side: Direction) = side == Direction.DOWN && slot == REPAIR_SLOT && !stack.isDamaged

    override fun <T : Any> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (!isRemoved && cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            return itemHandlers[if (side == Direction.UP) 0 else if (side == Direction.DOWN) 1 else 2].cast()
        }
        return super.getCapability(cap, side)
    }
    override fun invalidateCaps() { super.invalidateCaps(); itemHandlers.forEach { it.invalidate() } }
    override fun reviveCaps() { super.reviveCaps(); itemHandlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH) }
    override fun getDisplayName(): Component = Component.translatable("block.superbwarfare.weapon_repair_station")
    override fun createMenu(id: Int, inventory: Inventory, player: Player) = WeaponRepairStationMenu(id, inventory, this, data)

    companion object {
        const val MATERIAL_SLOT = 0
        const val REPAIR_SLOT = 1
        val capacity get() = MiscConfig.WEAPON_REPAIR_STATION_CAPACITY.get()
        val energyPerMaterial get() = MiscConfig.WEAPON_REPAIR_STATION_ENERGY_PER_MATERIAL.get()
        val energyPerDurability get() = MiscConfig.WEAPON_REPAIR_STATION_ENERGY_COST.get()
        val repairInterval get() = MiscConfig.WEAPON_REPAIR_STATION_REPAIR_INTERVAL.get()
        fun isMaterial(stack: ItemStack) = stack.`is`(ModItems.VEHICLE_RESET_KIT.get())
        fun isRepairTarget(stack: ItemStack) = !stack.isEmpty && stack.count == 1 && stack.isDamageableItem
        fun canRepair(stack: ItemStack) = isRepairTarget(stack) && stack.isDamaged
        fun serverTick(level: Level, pos: BlockPos, state: BlockState, station: WeaponRepairStationBlockEntity) {
            if (!level.isClientSide) station.tickRepair()
        }
    }
}
