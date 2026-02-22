package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.init.ModMenuTypes
import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class SuperbItemInterfaceMenu @JvmOverloads constructor(
    containerId: Int, playerInventory: Inventory, private val container: Container = SimpleContainer(CONTAINER_SIZE)
) : AbstractContainerMenu(ModMenuTypes.SUPERB_ITEM_INTERFACE_MENU.get(), containerId) {
    init {
        checkContainerSize(container, CONTAINER_SIZE)
        container.startOpen(playerInventory.player)

        for (j in 0..<CONTAINER_SIZE) {
            this.addSlot(Slot(container, j, 44 + j * 18, 20))
        }

        for (l in 0..2) {
            for (k in 0..8) {
                this.addSlot(Slot(playerInventory, k + l * 9 + 9, 8 + k * 18, l * 18 + 51))
            }
        }

        for (i1 in 0..8) {
            this.addSlot(Slot(playerInventory, i1, 8 + i1 * 18, 109))
        }
    }

    override fun stillValid(player: Player): Boolean {
        return this.container.stillValid(player)
    }

    override fun quickMoveStack(player: Player, index: Int): ItemStack {
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

    override fun removed(player: Player) {
        super.removed(player)
        this.container.stopOpen(player)
    }

    companion object {
        const val CONTAINER_SIZE: Int = 5
    }
}