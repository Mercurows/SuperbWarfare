package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.inventory.menu.WeaponRepairStationMenu
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.network.chat.Component
import net.minecraft.world.*
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
import net.minecraftforge.energy.EnergyStorage
import net.minecraftforge.items.wrapper.SidedInvWrapper

open class WeaponRepairStationBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.WEAPON_REPAIR_STATION.get(), pos, state), WorldlyContainer, MenuProvider {
    private var items = NonNullList.withSize(1, ItemStack.EMPTY)
    private var legacyMaterial = ItemStack.EMPTY
    var energy = 0
        private set

    private val energyStorage =
        object : EnergyStorage(feCapacity, MiscConfig.WEAPON_REPAIR_STATION_FE_TRANSFER_RATE.get(), 0) {
            override fun receiveEnergy(maxReceive: Int, simulate: Boolean): Int {
                val incoming = minOf(maxReceive.coerceAtLeast(0), this.maxReceive)
                if (incoming == 0) return 0
                // Fill the repair reserve before buffering FE; use Long for configurable Int limits.
                val total = this.energy.toLong() + incoming
                val cost = fePerRepairEnergy.toLong()
                val converted = minOf(
                    (WeaponRepairStationBlockEntity.capacity - this@WeaponRepairStationBlockEntity.energy).coerceAtLeast(
                        0
                    ).toLong(), total / cost
                )
                val stored = minOf(feCapacity.toLong(), total - converted * cost)
                val accepted = (converted * cost + stored - this.energy).coerceIn(0, incoming.toLong()).toInt()
                if (!simulate && accepted > 0) {
                    this@WeaponRepairStationBlockEntity.energy += converted.toInt()
                    this.energy = stored.toInt()
                    setChanged()
                }
                return accepted
            }

            override fun getMaxEnergyStored() = feCapacity
            fun clampFE() {
                this.energy = this.energy.coerceIn(0, feCapacity)
            }

            fun consumeFE(amount: Int) {
                this.energy -= amount
            }
        }

    private var energyHandler = LazyOptional.of { energyStorage }

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
                6 -> energyStorage.energyStored
                7 -> feCapacity
                8 -> fePerRepairEnergy
                else -> 0
            }
            return (value ushr ((index % 2) * 16)) and 65535
        }

        override fun set(index: Int, value: Int) = Unit // Server authority; the client owns a SimpleContainerData.
        override fun getCount() = WeaponRepairStationMenu.DATA_COUNT
    }

    private fun tickRepair() {
        var changed = false
        returnLegacyMaterial()
        val previousEnergy = energy
        val previousFE = energyStorage.energyStored
        energy = energy.coerceIn(0, capacity)
        energyStorage.clampFE()
        val converted = minOf(capacity - energy, energyStorage.energyStored / fePerRepairEnergy)
        energy += converted
        energyStorage.consumeFE(converted * fePerRepairEnergy)
        if (energy != previousEnergy || energyStorage.energyStored != previousFE) changed = true
        val target = items[REPAIR_SLOT]
        if (!canRepair(target)) {
            if (progress != 0) {
                progress = 0; changed = true
            }
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

    fun toggleEnabled() {
        enabled = !enabled; setChanged()
    }

    fun returnLegacyMaterial() {
        val world = level ?: return
        if (world.isClientSide || legacyMaterial.isEmpty) return
        Containers.dropItemStack(world, blockPos.x + 0.5, blockPos.y + 1.0, blockPos.z + 0.5, legacyMaterial)
        legacyMaterial = ItemStack.EMPTY
        setChanged()
    }

    override fun saveAdditional(tag: CompoundTag) {
        super.saveAdditional(tag)
        ContainerHelper.saveAllItems(tag, items)
        tag.putInt("InventoryVersion", 2)
        if (!legacyMaterial.isEmpty) tag.put("LegacyMaterial", legacyMaterial.save(CompoundTag()))
        tag.putInt("RepairEnergy", energy)
        tag.put("Energy", energyStorage.serializeNBT())
        tag.putInt("RepairProgress", progress)
        tag.putBoolean("RepairEnabled", enabled)
    }

    override fun load(tag: CompoundTag) {
        super.load(tag)
        items = NonNullList.withSize(1, ItemStack.EMPTY)
        if (tag.getInt("InventoryVersion") < 2) {
            val oldItems = NonNullList.withSize(2, ItemStack.EMPTY)
            ContainerHelper.loadAllItems(tag, oldItems)
            legacyMaterial = oldItems[0]
            items[REPAIR_SLOT] = oldItems[1]
        } else {
            ContainerHelper.loadAllItems(tag, items)
            legacyMaterial = ItemStack.of(tag.getCompound("LegacyMaterial"))
        }
        energy = tag.getInt("RepairEnergy").coerceIn(0, capacity)
        energyStorage.deserializeNBT(tag.get("Energy") ?: IntTag.valueOf(0))
        energyStorage.clampFE()
        progress = tag.getInt("RepairProgress").coerceAtLeast(0)
        enabled = !tag.contains("RepairEnabled") || tag.getBoolean("RepairEnabled")
    }

    override fun getContainerSize() = 1
    override fun isEmpty() = items.all { it.isEmpty }
    override fun getItem(slot: Int): ItemStack = items[slot]
    override fun removeItem(slot: Int, amount: Int): ItemStack = ContainerHelper.removeItem(items, slot, amount).also {
        if (!it.isEmpty) {
            if (slot == REPAIR_SLOT) progress = 0; setChanged()
        }
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

    override fun clearContent() {
        items.clear(); progress = 0; setChanged()
    }

    override fun stillValid(player: Player) = Container.stillValidBlockEntity(this, player)
    override fun canPlaceItem(slot: Int, stack: ItemStack) = slot == REPAIR_SLOT && isRepairTarget(stack)
    override fun getSlotsForFace(side: Direction): IntArray = intArrayOf(REPAIR_SLOT)
    override fun canPlaceItemThroughFace(slot: Int, stack: ItemStack, side: Direction?) =
        side != Direction.DOWN && canPlaceItem(slot, stack)

    override fun canTakeItemThroughFace(slot: Int, stack: ItemStack, side: Direction) =
        side == Direction.DOWN && slot == REPAIR_SLOT && !stack.isDamaged

    override fun <T : Any> getCapability(cap: Capability<T>, side: Direction?): LazyOptional<T> {
        if (!isRemoved && cap == ForgeCapabilities.ENERGY) return energyHandler.cast()
        if (!isRemoved && cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            return itemHandlers[if (side == Direction.UP) 0 else if (side == Direction.DOWN) 1 else 2].cast()
        }
        return super.getCapability(cap, side)
    }

    override fun invalidateCaps() {
        super.invalidateCaps(); energyHandler.invalidate(); itemHandlers.forEach { it.invalidate() }
    }

    override fun reviveCaps() {
        super.reviveCaps()
        energyHandler = LazyOptional.of { energyStorage }
        itemHandlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH)
    }

    override fun getDisplayName(): Component = Component.translatable("block.superbwarfare.weapon_repair_station")
    override fun createMenu(id: Int, inventory: Inventory, player: Player) =
        WeaponRepairStationMenu(id, inventory, this, data)

    companion object {
        const val REPAIR_SLOT = 0
        val capacity: Int get() = MiscConfig.WEAPON_REPAIR_STATION_CAPACITY.get()
        val feCapacity: Int get() = MiscConfig.WEAPON_REPAIR_STATION_MAX_FE.get()
        val fePerRepairEnergy: Int get() = MiscConfig.WEAPON_REPAIR_STATION_FE_PER_REPAIR_ENERGY.get()
        val energyPerDurability: Int get() = MiscConfig.WEAPON_REPAIR_STATION_ENERGY_COST.get()
        val repairInterval: Int get() = MiscConfig.WEAPON_REPAIR_STATION_REPAIR_INTERVAL.get()

        fun isRepairTarget(stack: ItemStack) = !stack.isEmpty && stack.count == 1 && stack.isDamageableItem

        fun canRepair(stack: ItemStack) = isRepairTarget(stack) && stack.isDamaged

        fun serverTick(level: Level, pos: BlockPos, state: BlockState, station: WeaponRepairStationBlockEntity) {
            if (!level.isClientSide) station.tickRepair()
        }
    }
}
