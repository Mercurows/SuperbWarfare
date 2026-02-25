package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.init.ModMenuTypes
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.ContainerData
import net.minecraft.world.inventory.SimpleContainerData
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class BlueprintResearchTableMenu(
    containerId: Int,
    playerInventory: Inventory,
    private val container: Container,
    private val containerData: ContainerData = SimpleContainerData(BlueprintResearchTableBlockEntity.MAX_DATA_COUNT)
) : AbstractContainerMenu(ModMenuTypes.BLUEPRINT_RESEARCH_TABLE.get(), containerId) {
    constructor(containerId: Int, playerInventory: Inventory) : this(
        containerId,
        playerInventory,
        SimpleContainer(CONTAINER_SIZE)
    )

    init {
        checkContainerSize(this.container, CONTAINER_SIZE)
        checkContainerDataCount(this.containerData, BlueprintResearchTableBlockEntity.MAX_DATA_COUNT)

        this.addSlot(FuelSlot(this.container, SLOT_FUEL, 31, 21))
        this.addSlot(Slot(this.container, SLOT_INPUT, 31, 50))
        this.addSlot(Slot(this.container, SLOT_INPUT_BASE, 119, 21))
        this.addSlot(Slot(this.container, SLOT_INPUT_DYE, 139, 21))
        this.addSlot(Slot(this.container, SLOT_SPECIAL, 80, 50))
        this.addSlot(ResultSlot(this.container, SLOT_OUTPUT, 129, 50))

        this.addDataSlots(this.containerData)

        for (i in 0..2) {
            for (j in 0..8) {
                this.addSlot(
                    Slot(
                        playerInventory,
                        j + i * 9 + 9,
                        8 + j * 18,
                        95 + i * 18
                    )
                )
            }
        }

        for (k in 0..8) {
            this.addSlot(
                Slot(
                    playerInventory,
                    k,
                    8 + k * 18,
                    153
                )
            )
        }
    }

    override fun quickMoveStack(
        player: Player,
        index: Int
    ): ItemStack {
        var stack = ItemStack.EMPTY
        val slot = this.slots[index]
        if (slot.hasItem()) {
            val slotItem = slot.item
            stack = slotItem.copy()
            if (index < this.container.containerSize) {
                if (!this.moveItemStackTo(slotItem, this.container.containerSize, this.slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else if (!this.moveItemStackTo(slotItem, 0, this.container.containerSize - 1, false)) {
                return ItemStack.EMPTY
            }

            if (slotItem.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }
        }

        return stack
    }

    override fun stillValid(pPlayer: Player): Boolean {
        return this.container.stillValid(pPlayer)
    }

    fun getTick() = this.containerData.get(0)

    fun getLastSelectedIndex() = this.containerData.get(1)

    fun setLastSelectedIndex(index: Int) = this.containerData.set(1, index)

    fun getFuel() = this.containerData.get(2)

    fun getMaxProcessTick() = this.containerData.get(3)

    fun isActivated() = this.containerData.get(4) == 1

    fun setActivated(flag: Boolean) = this.containerData.set(4, if (flag) 1 else 0)

    companion object {
        const val CONTAINER_SIZE = 6
        const val SLOT_FUEL = 0
        const val SLOT_INPUT = 1
        const val SLOT_INPUT_BASE = 2
        const val SLOT_INPUT_DYE = 3
        const val SLOT_SPECIAL = 4
        const val SLOT_OUTPUT = 5
    }

    private class ResultSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(pStack: ItemStack): Boolean {
            return false
        }
    }

    private class FuelSlot(container: Container, slot: Int, x: Int, y: Int) : Slot(container, slot, x, y) {
        override fun mayPlace(pStack: ItemStack): Boolean {
            return pStack.`is`(ModTags.Items.RESEARCH_FUEL)
        }
    }
}