package com.atsuishio.superbwarfare.menu

import com.atsuishio.superbwarfare.init.ModMenuTypes
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.ItemStack

class BlueprintResearchTableMenu(containerId: Int, playerInventory: Inventory, private val container: Container) :
    AbstractContainerMenu(ModMenuTypes.BLUEPRINT_RESEARCH_TABLE.get(), containerId) {

    constructor(containerId: Int, playerInventory: Inventory) : this(
        containerId,
        playerInventory,
        SimpleContainer(CONTAINER_SIZE)
    )

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
            } else if (!this.moveItemStackTo(slotItem, 0, this.container.containerSize, false)) {
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

    companion object {
        const val CONTAINER_SIZE = 6
    }
}