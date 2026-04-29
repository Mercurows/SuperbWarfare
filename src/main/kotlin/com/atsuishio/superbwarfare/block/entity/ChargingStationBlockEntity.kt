package com.atsuishio.superbwarfare.block.entity

import com.atsuishio.superbwarfare.block.ChargingStationBlock
import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModDataComponents
import com.atsuishio.superbwarfare.inventory.menu.ChargingStationMenu
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData
import com.atsuishio.superbwarfare.tools.isSameItemStack
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.HolderLookup
import net.minecraft.core.NonNullList
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
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
import net.neoforged.neoforge.capabilities.Capabilities
import net.neoforged.neoforge.energy.EnergyStorage
import net.neoforged.neoforge.energy.IEnergyStorage
import javax.annotation.ParametersAreNonnullByDefault
import kotlin.math.min

/**
 * Energy Data Slot Code based on @GoryMoon's Chargers
 */
open class ChargingStationBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(ModBlockEntities.CHARGING_STATION.get(), pos, state), WorldlyContainer, MenuProvider {

    protected var items: NonNullList<ItemStack> = NonNullList.withSize(2, ItemStack.EMPTY)

    var fuelTick = 0
    var maxFuelTick = DEFAULT_FUEL_TIME
    var showRange = false

    protected val dataAccess: ContainerEnergyData = object : ContainerEnergyData {
        override fun get(index: Int): Long {
            return when (index) {
                0 -> this@ChargingStationBlockEntity.fuelTick
                1 -> this@ChargingStationBlockEntity.maxFuelTick
                2 -> {
                    val level = this@ChargingStationBlockEntity.level ?: return 0

                    val cap = level.getCapability(
                        Capabilities.EnergyStorage.BLOCK,
                        this@ChargingStationBlockEntity.blockPos,
                        null
                    )
                    if (cap == null) return 0

                    cap.energyStored
                }

                3 -> if (this@ChargingStationBlockEntity.showRange) 1 else 0
                else -> 0
            }.toLong()
        }

        override fun set(index: Int, value: Long) {
            when (index) {
                0 -> this@ChargingStationBlockEntity.fuelTick = value.toInt()
                1 -> this@ChargingStationBlockEntity.maxFuelTick = value.toInt()
                2 -> {
                    val level = this@ChargingStationBlockEntity.level ?: return

                    val cap = level.getCapability(
                        Capabilities.EnergyStorage.BLOCK,
                        this@ChargingStationBlockEntity.blockPos,
                        null
                    )
                    if (cap == null) return

                    cap.receiveEnergy(value.toInt(), false)
                }

                3 -> this@ChargingStationBlockEntity.showRange = value == 1L
            }
        }

        override fun getCount(): Int {
            return MAX_DATA_COUNT
        }
    }

    private fun chargeEntity(handler: IEnergyStorage) {
        val level = this.level ?: return
        if (level.gameTime % 20 != 0L) return

        val entities: MutableList<Entity?> = level.getEntitiesOfClass<Entity?>(
            Entity::class.java,
            AABB(this.blockPos).inflate(CHARGE_RADIUS.toDouble())
        )
        entities.forEach { entity ->
            val cap = entity?.getCapability(Capabilities.EnergyStorage.ENTITY, null)
            if (cap == null || !cap.canReceive()) return@forEach

            val charged = cap.receiveEnergy(min(handler.energyStored, CHARGE_OTHER_SPEED * 20), false)
            handler.extractEnergy(charged, false)
        }
        this.setChanged()
    }

    private fun chargeItemStack(handler: IEnergyStorage) {
        val stack: ItemStack = this.getItem(SLOT_CHARGE)
        if (stack.isEmpty) return

        val consumer = stack.getCapability(Capabilities.EnergyStorage.ITEM)
        if (consumer != null) {
            if (consumer.energyStored < consumer.maxEnergyStored) {
                val charged = consumer.receiveEnergy(min(CHARGE_OTHER_SPEED, handler.energyStored), false)
                handler.extractEnergy(min(charged, handler.energyStored), false)
            }
        }
        this.setChanged()
    }

    private fun chargeBlock(handler: IEnergyStorage) {
        val level = this.level ?: return

        for (direction in Direction.entries) {
            val blockEntity = level.getBlockEntity(this.blockPos.relative(direction)) ?: continue

            val energy = level.getCapability(
                Capabilities.EnergyStorage.BLOCK,
                blockEntity.blockPos,
                direction
            )
            if (energy == null || blockEntity is ChargingStationBlockEntity) continue

            if (energy.canReceive() && energy.energyStored < energy.maxEnergyStored) {
                val receiveEnergy = energy.receiveEnergy(min(handler.energyStored, CHARGE_OTHER_SPEED), false)
                handler.extractEnergy(receiveEnergy, false)

                blockEntity.setChanged()
                this.setChanged()
            }
        }
    }

    override fun applyImplicitComponents(componentInput: DataComponentInput) {
        super.applyImplicitComponents(componentInput)

        val level = this.level
        if (level != null) {
            (this.energyStorage as EnergyStorage).deserializeNBT(
                level.registryAccess(),
                IntTag.valueOf(componentInput.getOrDefault(ModDataComponents.ENERGY, 0))
            )
        }
    }

    override fun collectImplicitComponents(components: DataComponentMap.Builder) {
        super.collectImplicitComponents(components)

        components.set(ModDataComponents.ENERGY, this.energyStorage.energyStored)
    }

    override fun loadAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.loadAdditional(tag, registries)

        if (tag.contains("Energy")) {
            val energy = tag.get("Energy")
            if (energy is IntTag) {
                (this.energyStorage as EnergyStorage).deserializeNBT(registries, energy)
            }
        }
        this.fuelTick = tag.getInt("FuelTick")
        this.maxFuelTick = tag.getInt("MaxFuelTick")
        this.showRange = tag.getBoolean("ShowRange")
        this.items = NonNullList.withSize(this.containerSize, ItemStack.EMPTY)
        ContainerHelper.loadAllItems(tag, this.items, registries)
    }

    override fun saveAdditional(tag: CompoundTag, registries: HolderLookup.Provider) {
        super.saveAdditional(tag, registries)

        tag.putInt("Energy", this.energyStorage.energyStored)

        tag.putInt("FuelTick", this.fuelTick)
        tag.putInt("MaxFuelTick", this.maxFuelTick)
        tag.putBoolean("ShowRange", this.showRange)
        ContainerHelper.saveAllItems(tag, this.items, registries)
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
        val itemstack: ItemStack = this.items[pSlot]
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

    override fun getUpdateTag(registries: HolderLookup.Provider): CompoundTag {
        val compoundtag = CompoundTag()
        ContainerHelper.saveAllItems(compoundtag, this.items, registries)
        compoundtag.putBoolean("ShowRange", this.showRange)
        return compoundtag
    }

    @ParametersAreNonnullByDefault
    override fun saveToItem(stack: ItemStack, registries: HolderLookup.Provider) {
        val tag = CompoundTag()
        if (this.level != null) {
            tag.put("Energy", (energyStorage as EnergyStorage).serializeNBT(registries))
        }
        BlockItem.setBlockEntityData(stack, this.type, tag)
    }

    private val energyStorage: IEnergyStorage = EnergyStorage(MAX_ENERGY)

    fun getEnergyStorage(side: Direction?): IEnergyStorage {
        return energyStorage
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

        @JvmStatic
        fun serverTick(
            pLevel: Level,
            pPos: BlockPos,
            pState: BlockState,
            blockEntity: ChargingStationBlockEntity
        ) {
            if (blockEntity.showRange != pState.getValue(ChargingStationBlock.SHOW_RANGE)) {
                pLevel.setBlockAndUpdate(
                    pPos,
                    pState.setValue(ChargingStationBlock.SHOW_RANGE, blockEntity.showRange)
                )
                setChanged(pLevel, pPos, pState)
            }

            val handler = blockEntity.getEnergyStorage(null)

            val energy = handler.energyStored
            if (energy > 0) {
                blockEntity.chargeEntity(handler)
            }
            if (handler.energyStored > 0) {
                blockEntity.chargeItemStack(handler)
            }
            if (handler.energyStored > 0) {
                blockEntity.chargeBlock(handler)
            }

            if (blockEntity.fuelTick > 0) {
                blockEntity.fuelTick--
                if (energy < handler.maxEnergyStored) {
                    handler.receiveEnergy(CHARGE_SPEED, false)
                }
            } else if (!blockEntity.getItem(SLOT_FUEL).isEmpty) {
                if (handler.energyStored >= handler.maxEnergyStored) return

                val fuel: ItemStack = blockEntity.getItem(SLOT_FUEL)
                val burnTime = fuel.getBurnTime(RecipeType.SMELTING)

                val fuelEnergy = fuel.getCapability(Capabilities.EnergyStorage.ITEM)

                if (fuelEnergy != null) {
                    // 优先当作电池处理
                    val energyToExtract = min(CHARGE_OTHER_SPEED, handler.maxEnergyStored - handler.energyStored)
                    if (fuelEnergy.canExtract() && handler.canReceive()) {
                        handler.receiveEnergy(fuelEnergy.extractEnergy(energyToExtract, false), false)
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
                } else if (fuel.get(DataComponents.FOOD) != null) {
                    // 最后作为食物处理
                    val foodComponent = fuel.get(DataComponents.FOOD) ?: return

                    val nutrition = foodComponent.nutrition()
                    val saturation = foodComponent.saturation() * 2.0f * nutrition
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