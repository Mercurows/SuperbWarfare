package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.SlotItemHandler

abstract class AbstractVehicleContainerMenu(type: MenuType<*>?, id: Int, inventory: Inventory, entityId: Int) :
    AbstractContainerMenu(type, id) {
    val vehicle: VehicleEntity? = inventory.player.level().getEntity(entityId) as? VehicleEntity

    init {
        if (vehicle != null) {
            this.addPlayerInventory(inventory)
            this.addVehicleInventory()
        }
    }

    open fun addPlayerInventory(inventory: Inventory) {
        val i = (getRows() - 4) * 18
        for (r in 0 until 3) {
            for (c in 0 until 9) {
                this.addSlot(Slot(inventory, c + r * 9 + 9, 8 + c * 18, 103 + r * 18 + i))
            }
        }
        for (c in 0 until 9) {
            this.addSlot(Slot(inventory, c, 8 + c * 18, 161 + i))
        }
    }

    override fun quickMoveStack(
        player: Player,
        index: Int
    ): ItemStack {
        var stack1 = ItemStack.EMPTY
        val slot = this.slots[index]
        if (slot.hasItem()) {
            val stack2 = slot.item
            stack1 = stack2.copy()
            if (index < 36) {
                if (!this.moveItemStackTo(stack2, 36, this.slots.size, false)) {
                    return ItemStack.EMPTY
                }
            } else if (!this.moveItemStackTo(stack2, 0, 36, true)) {
                return ItemStack.EMPTY
            }

            if (stack2.isEmpty) {
                slot.setByPlayer(ItemStack.EMPTY)
            } else {
                slot.setChanged()
            }

            if (stack2.count == stack1.count) {
                return ItemStack.EMPTY
            }

            slot.onTake(player, stack2)
        }
        return stack1
    }

    abstract fun addVehicleInventory()

    open fun getRows(): Int = 0

    override fun stillValid(pPlayer: Player): Boolean {
        if (vehicle == null) return false
        return vehicle.isAlive && pPlayer.canReach(this.vehicle, 3.0)
    }

    class VehicleSlot(private val vehicle: VehicleEntity?, val index: Int, x: Int, y: Int) :
        SlotItemHandler(vehicle?.inventory, index, x, y) {
        override fun mayPickup(playerIn: Player): Boolean {
            return this.vehicle?.canTakeItem(index) ?: false
        }
    }
}