package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.ChargingStationBlock
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.inventory.menu.ChargingStationMenu
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData
import com.atsuishio.superbwarfare.tools.isSameItemStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket
import net.minecraft.world.Container
import net.minecraft.world.ContainerHelper
import net.minecraft.world.MenuProvider
import net.minecraft.world.WorldlyContainer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.capabilities.ForgeCapabilities
import net.minecraftforge.common.util.LazyOptional
import net.minecraftforge.energy.EnergyStorage
import net.minecraftforge.items.wrapper.SidedInvWrapper
import kotlin.math.min

/**
 * Energy Data Slot Code based on @GoryMoon's Chargers
 */
open class ChargingStationBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.CHARGING_STATION.get(), pos, state), WorldlyContainer, MenuProvider {
    var items: NonNullList<ItemStack> = NonNullList.withSize(2, ItemStack.EMPTY)
        protected set

    private var energyHandler: LazyOptional<EnergyStorage>
    private var itemHandlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH)

    var fuelTick: Int = 0
    var maxFuelTick: Int = DEFAULT_FUEL_TIME
    var showRange: Boolean = false

    protected val dataAccess: ContainerEnergyData = object : ContainerEnergyData {
        override fun get(index: Int): Long {
            return when (index) {
                0 -> this@ChargingStationBlockEntity.fuelTick.toLong()
                1 -> this@ChargingStationBlockEntity.maxFuelTick.toLong()
                2 -> {
                    val energy = intArrayOf(0)
                    this@ChargingStationBlockEntity.getCapability(ForgeCapabilities.ENERGY)
                        .ifPresent { energy[0] = it.energyStored }
                    energy[0].toLong()
                }

                3 -> if (this@ChargingStationBlockEntity.showRange) 1L else 0L
                else -> 0L
            }
        }

        override fun set(index: Int, value: Long) {
            when (index) {
                0 -> this@ChargingStationBlockEntity.fuelTick = value.toInt()
                1 -> this@ChargingStationBlockEntity.maxFuelTick = value.toInt()
                2 -> this@ChargingStationBlockEntity.getCapability(ForgeCapabilities.ENERGY)
                    .ifPresent {
                        it.receiveEnergy(value.toInt(), false)
                    }

                3 -> this@ChargingStationBlockEntity.showRange = value == 1L
            }
        }

        override fun getCount(): Int {
            return MAX_DATA_COUNT
        }
    }

    init {
        this.energyHandler = LazyOptional.of { EnergyStorage(MAX_ENERGY) }
    }

    private fun chargeEntity(handler: EnergyStorage) {
        if (this.level == null) return
        if (this.level!!.gameTime % 20 != 0L) return

        val entities = this.level!!.getEntitiesOfClass<Entity>(
            Entity::class.java, AABB(this.blockPos)
                .inflate(CHARGE_RADIUS.toDouble())
        )
        entities.forEach { entity ->
            entity.getCapability(ForgeCapabilities.ENERGY)
                .ifPresent { cap ->
                    if (cap.canReceive()) {
                        val charged =
                            cap.receiveEnergy(min(handler.energyStored, CHARGE_OTHER_SPEED * 20), false)
                        handler.extractEnergy(charged, false)
                    }
                }
        }
        this.setChanged()
    }

    private fun chargeItemStack(handler: EnergyStorage) {
        val stack = this.getItem(SLOT_CHARGE)
        if (stack.isEmpty) return

        stack.getCapability(ForgeCapabilities.ENERGY)
            .ifPresent {
                if (it.energyStored < it.maxEnergyStored) {
                    val charged = it.receiveEnergy(min(CHARGE_OTHER_SPEED, handler.energyStored), false)
                    handler.extractEnergy(min(charged, handler.energyStored), false)
                }
            }
        this.setChanged()
    }

    private fun chargeBlock(handler: EnergyStorage) {
        if (this.level == null) return

        for (direction in Direction.entries) {
            val blockEntity = this.level!!.getBlockEntity(this.blockPos.relative(direction))
            if (blockEntity == null
                || !blockEntity.getCapability(ForgeCapabilities.ENERGY).isPresent
                || blockEntity is ChargingStationBlockEntity
            ) {
                continue
            }

            blockEntity.getCapability(ForgeCapabilities.ENERGY)
                .ifPresent {
                    if (it.canReceive() && it.energyStored < it.maxEnergyStored) {
                        val receiveEnergy =
                            it.receiveEnergy(min(handler.energyStored, CHARGE_OTHER_SPEED), false)
                        handler.extractEnergy(receiveEnergy, false)

                        blockEntity.setChanged()
                        this.setChanged()
                    }
                }
        }
    }

    override fun load(pTag: CompoundTag) {
        super.load(pTag)

        if (pTag.contains("Energy")) {
            getCapability(ForgeCapabilities.ENERGY).ifPresent {
                (it as EnergyStorage).deserializeNBT(pTag.get("Energy"))
            }
        }
        this.fuelTick = pTag.getInt("FuelTick")
        this.maxFuelTick = pTag.getInt("MaxFuelTick")
        this.showRange = pTag.getBoolean("ShowRange")
        this.items = NonNullList.withSize(this.containerSize, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(pTag, this.items)
    }

    override fun saveAdditional(pTag: CompoundTag) {
        super.saveAdditional(pTag)

        getCapability(ForgeCapabilities.ENERGY).ifPresent {
            pTag.put("Energy", (it as EnergyStorage).serializeNBT())
        }
        pTag.putInt("FuelTick", this.fuelTick)
        pTag.putInt("MaxFuelTick", this.maxFuelTick)
        pTag.putBoolean("ShowRange", this.showRange)
        ContainerHelper.saveAllItems(pTag, this.items)
    }

    override fun getSlotsForFace(pSide: Direction): IntArray {
        return intArrayOf(SLOT_FUEL)
    }

    override fun canPlaceItemThroughFace(pIndex: Int, pItemStack: ItemStack, pDirection: Direction?): Boolean {
        return pIndex == SLOT_FUEL
    }

    override fun canTakeItemThroughFace(pIndex: Int, pStack: ItemStack, pDirection: Direction): Boolean {
        return false
    }

    override fun getContainerSize(): Int {
        return this.items.size
    }

    override fun isEmpty(): Boolean {
        for (itemstack in this.items) {
            if (!itemstack.isEmpty) {
                return false
            }
        }

        return true
    }

    override fun getItem(pSlot: Int): ItemStack {
        return this.items[pSlot]
    }

    override fun removeItem(pSlot: Int, pAmount: Int): ItemStack {
        return ContainerHelper.removeItem(this.items, pSlot, pAmount)
    }

    override fun removeItemNoUpdate(pSlot: Int): ItemStack {
        return ContainerHelper.takeItem(this.items, pSlot)
    }

    override fun setItem(pSlot: Int, pStack: ItemStack) {
        val itemstack = this.items[pSlot]
        val flag = !pStack.isEmpty && isSameItemStack(itemstack, pStack)
        this.items[pSlot] = pStack
        if (pStack.count > this.maxStackSize) {
            pStack.count = this.maxStackSize
        }

        if (pSlot == 0 && !flag) {
            this.setChanged()
        }
    }

    override fun stillValid(pPlayer: Player): Boolean {
        return Container.stillValidBlockEntity(this, pPlayer)
    }

    override fun clearContent() {
        this.items.clear()
    }

    override fun getDisplayName(): Component {
        return Component.translatable("container.superbwarfare.charging_station")
    }

    override fun createMenu(pContainerId: Int, pPlayerInventory: Inventory, pPlayer: Player): AbstractContainerMenu? {
        return ChargingStationMenu(pContainerId, pPlayerInventory, this, this.dataAccess)
    }

    override fun getUpdatePacket(): ClientboundBlockEntityDataPacket? {
        return ClientboundBlockEntityDataPacket.create(this)
    }

    override fun getUpdateTag(): CompoundTag {
        val tag = CompoundTag()
        ContainerHelper.saveAllItems(tag, this.items, true)
        tag.putBoolean("ShowRange", this.showRange)
        return tag
    }

    override fun <T> getCapability(cap: Capability<T?>, side: Direction?): LazyOptional<T?> {
        if (cap === ForgeCapabilities.ENERGY) {
            return energyHandler.cast<T?>()
        }
        if (!this.remove && side != null && cap === ForgeCapabilities.ITEM_HANDLER) {
            return when (side) {
                Direction.UP -> {
                    itemHandlers[0].cast<T?>()
                }

                Direction.DOWN -> {
                    itemHandlers[1].cast<T?>()
                }

                else -> {
                    itemHandlers[2].cast<T?>()
                }
            }
        }
        return super.getCapability<T?>(cap, side)
    }

    override fun invalidateCaps() {
        super.invalidateCaps()
        for (itemHandler in itemHandlers) itemHandler.invalidate()
    }

    override fun reviveCaps() {
        super.reviveCaps()
        this.itemHandlers = SidedInvWrapper.create(this, Direction.UP, Direction.DOWN, Direction.NORTH)
        this.energyHandler = LazyOptional.of { EnergyStorage(MAX_ENERGY) }
    }

    override fun saveToItem(pStack: ItemStack) {
        val tag = CompoundTag()
        this.getCapability(ForgeCapabilities.ENERGY)
            .ifPresent {
                tag.put("Energy", (it as EnergyStorage).serializeNBT())
            }
        BlockItem.setBlockEntityData(pStack, this.type, tag)
    }

    companion object {
        protected const val SLOT_FUEL: Int = 0
        protected const val SLOT_CHARGE: Int = 1
        const val MAX_DATA_COUNT: Int = 4

        @JvmField
        val MAX_ENERGY: Int = MiscConfig.CHARGING_STATION_MAX_ENERGY.get()

        @JvmField
        val DEFAULT_FUEL_TIME: Int = MiscConfig.CHARGING_STATION_DEFAULT_FUEL_TIME.get()

        @JvmField
        val CHARGE_SPEED: Int = MiscConfig.CHARGING_STATION_GENERATE_SPEED.get()

        @JvmField
        val CHARGE_OTHER_SPEED: Int = MiscConfig.CHARGING_STATION_TRANSFER_SPEED.get()

        @JvmField
        val CHARGE_RADIUS: Int = MiscConfig.CHARGING_STATION_CHARGE_RADIUS.get()

        fun serverTick(pLevel: Level, pPos: BlockPos, pState: BlockState, blockEntity: ChargingStationBlockEntity) {
            if (blockEntity.showRange != pState.getValue(ChargingStationBlock.SHOW_RANGE)) {
                pLevel.setBlockAndUpdate(
                    pPos,
                    pState.setValue(ChargingStationBlock.SHOW_RANGE, blockEntity.showRange)
                )
                setChanged(pLevel, pPos, pState)
            }

            blockEntity.energyHandler.ifPresent {
                val energy = it.energyStored
                if (energy > 0) {
                    blockEntity.chargeEntity(it)
                }
                if (it.energyStored > 0) {
                    blockEntity.chargeItemStack(it)
                }
                if (it.energyStored > 0) {
                    blockEntity.chargeBlock(it)
                }
            }

            if (blockEntity.fuelTick > 0) {
                blockEntity.fuelTick--
                blockEntity.energyHandler.ifPresent {
                    val energy = it.energyStored
                    if (energy < it.maxEnergyStored) {
                        it.receiveEnergy(CHARGE_SPEED, false)
                    }
                }
            } else if (!blockEntity.getItem(SLOT_FUEL).isEmpty) {
                var flag = false
                blockEntity.energyHandler.ifPresent {
                    if (it.energyStored >= it.maxEnergyStored) {
                        flag = true
                    }
                }
                if (flag) return

                val fuel = blockEntity.getItem(SLOT_FUEL)
                val burnTime = ForgeHooks.getBurnTime(fuel, RecipeType.SMELTING)

                if (fuel.getCapability(ForgeCapabilities.ENERGY).isPresent) {
                    // 优先当作电池处理
                    fuel.getCapability(ForgeCapabilities.ENERGY)
                        .ifPresent { itemEnergy ->
                            blockEntity.energyHandler.ifPresent { energy ->
                                val energyToExtract =
                                    min(CHARGE_OTHER_SPEED, energy.maxEnergyStored - energy.energyStored)
                                if (itemEnergy.canExtract() && energy.canReceive()) {
                                    energy.receiveEnergy(itemEnergy.extractEnergy(energyToExtract, false), false)
                                }
                            }
                        }
                    blockEntity.setChanged()
                } else if (burnTime > 0) {
                    // 其次尝试作为燃料处理
                    blockEntity.fuelTick = burnTime
                    blockEntity.maxFuelTick = burnTime

                    if (fuel.hasCraftingRemainingItem()) {
                        if (fuel.count <= 1) {
                            blockEntity.setItem(SLOT_FUEL, fuel.craftingRemainingItem)
                        } else {
                            val copy = fuel.craftingRemainingItem.copy()
                            copy.count = 1

                            val itemEntity = ItemEntity(
                                pLevel,
                                pPos.x + 0.5,
                                pPos.y + 0.2,
                                pPos.z + 0.5,
                                copy
                            )
                            pLevel.addFreshEntity(itemEntity)

                            fuel.shrink(1)
                        }
                    } else {
                        fuel.shrink(1)
                    }

                    blockEntity.setChanged()
                } else if (fuel.item.isEdible) {
                    // 最后作为食物处理
                    val properties = fuel.getFoodProperties(null) ?: return

                    val nutrition = properties.nutrition
                    val saturation = properties.saturationModifier * 2.0f * nutrition
                    var tick = nutrition * 80 + (saturation * 200).toInt()

                    if (fuel.hasCraftingRemainingItem()) {
                        tick += 400
                    }

                    fuel.shrink(1)

                    blockEntity.fuelTick = tick
                    blockEntity.maxFuelTick = tick
                    blockEntity.setChanged()
                }
            }
        }
    }
}
