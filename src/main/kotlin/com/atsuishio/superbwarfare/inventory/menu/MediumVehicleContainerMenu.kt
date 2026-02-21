package com.atsuishio.superbwarfare.inventory.menu

import com.github.tartaricacid.touhoulittlemaid.api.backpack.ITriggerSlotChange
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.item.ItemStack
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension

class MediumVehicleContainerMenu(id: Int, inventory: Inventory, entityId: Int) :
    AbstractVehicleContainerMenu(TYPE, id, inventory, entityId) {
    override fun getRows(): Int = 6

    override fun addVehicleInventory() {
        for (r in 0 until getRows()) {
            for (c in 0 until 9) {
                this.addSlot(VehicleSlot(this.vehicle, c + r * 9, 8 + c * 18, 18 + r * 18))
            }
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
            if (slot is ITriggerSlotChange) {
                val slotChange = slot as ITriggerSlotChange
                slotChange.onShiftTakeoff(player, stack1)
            }
        }
        return stack1
    }

    companion object {
        @JvmField
        val TYPE: MenuType<MediumVehicleContainerMenu> =
            IMenuTypeExtension.create { id, inventory, buf -> MediumVehicleContainerMenu(id, inventory, buf.readInt()) }
    }
}