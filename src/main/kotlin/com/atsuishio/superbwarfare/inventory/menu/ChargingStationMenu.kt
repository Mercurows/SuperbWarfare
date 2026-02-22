package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.block.entity.ChargingStationBlockEntity
import com.atsuishio.superbwarfare.init.ModMenuTypes
import com.atsuishio.superbwarfare.network.dataslot.ContainerEnergyData
import com.atsuishio.superbwarfare.network.dataslot.SimpleEnergyData
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.level.Level
import net.minecraftforge.common.ForgeHooks
import net.minecraftforge.common.capabilities.ForgeCapabilities

open class ChargingStationMenu @JvmOverloads constructor(
    id: Int,
    inventory: Inventory,
    container: Container = SimpleContainer(2),
    containerData: ContainerEnergyData = SimpleEnergyData(ChargingStationBlockEntity.MAX_DATA_COUNT)
) : EnergyMenu(ModMenuTypes.CHARGING_STATION_MENU.get(), id, containerData) {
    private val container: Container
    private val containerData: ContainerEnergyData
    protected val level: Level

    init {
        checkContainerSize(container, 2)

        this.container = container
        this.containerData = containerData
        this.level = inventory.player.level()

        this.addSlot(Slot(container, 0, 44, 54))
        this.addSlot(ChargingSlot(container, 1, 116, 54))

        for (i in 0..2) {
            for (j in 0..8) {
                this.addSlot(Slot(inventory, j + i * 9 + 9, 8 + j * 18 + X_OFFSET, 84 + i * 18 + Y_OFFSET))
            }
        }

        for (k in 0..8) {
            this.addSlot(Slot(inventory, k, 8 + k * 18 + X_OFFSET, 142 + Y_OFFSET))
        }
    }

    override fun quickMoveStack(pPlayer: Player, pIndex: Int): ItemStack {
        var itemstack = ItemStack.EMPTY
        val slot = this.slots[pIndex]
        if (slot.hasItem()) {
            val itemstack1 = slot.item
            itemstack = itemstack1.copy()
            if (pIndex == 1) {
                if (!this.moveItemStackTo(itemstack1, 2, 38, true)) {
                    return ItemStack.EMPTY
                }
            } else if (pIndex != 0) {
                if (itemstack1.getCapability(ForgeCapabilities.ENERGY).isPresent) {
                    if (!this.moveItemStackTo(itemstack1, 1, 2, true)) {
                        return ItemStack.EMPTY
                    }
                } else if (ForgeHooks.getBurnTime(itemstack1, RecipeType.SMELTING) > 0 || itemstack1.getFoodProperties(
                        null
                    ) != null
                ) {
                    if (!this.moveItemStackTo(itemstack1, 0, 1, false)) {
                        return ItemStack.EMPTY
                    }
                } else if (pIndex in 2..<29) {
                    if (!this.moveItemStackTo(itemstack1, 29, 38, false)) {
                        return ItemStack.EMPTY
                    }
                } else if (pIndex in 29..<38 && !this.moveItemStackTo(itemstack1, 2, 29, false)) {
                    return ItemStack.EMPTY
                }
            } else if (!this.moveItemStackTo(itemstack1, 2, 38, false)) {
                return ItemStack.EMPTY
            }

            if (itemstack1.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }

            if (itemstack1.count == itemstack.count) {
                return ItemStack.EMPTY
            }

            slot.onTake(pPlayer, itemstack1)
        }

        return itemstack
    }

    override fun stillValid(pPlayer: Player): Boolean {
        return this.container.stillValid(pPlayer)
    }

    val fuelTick: Long
        get() = this.containerData[0]

    val maxFuelTick: Long
        get() = this.containerData[1]

    val energy: Long
        get() = this.containerData[2]

    fun showRange(): Boolean {
        return this.containerData[3] == 1L
    }

    fun setShowRange(showRange: Boolean) {
        this.containerData[3] = if (showRange) 1L else 0L
    }

    internal class ChargingSlot(pContainer: Container, pSlot: Int, pX: Int, pY: Int) : Slot(pContainer, pSlot, pX, pY)
    companion object {
        const val X_OFFSET: Int = 0
        const val Y_OFFSET: Int = 0
    }
}