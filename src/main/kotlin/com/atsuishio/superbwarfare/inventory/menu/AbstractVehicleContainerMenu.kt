package com.atsuishio.superbwarfare.inventory.menu

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.minecraft.world.inventory.Slot
import net.neoforged.neoforge.items.SlotItemHandler

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

    abstract fun addVehicleInventory()

    open fun getRows(): Int = 3

    override fun stillValid(pPlayer: Player): Boolean {
        if (vehicle == null) return false
        return vehicle.isAlive && pPlayer.canInteractWithEntity(this.vehicle, 3.0)
    }

    class VehicleSlot(private val vehicle: VehicleEntity?, val index: Int, x: Int, y: Int) :
        SlotItemHandler(vehicle?.inventory, index, x, y) {
        override fun mayPickup(playerIn: Player): Boolean {
            return this.vehicle?.canTakeItem(index) ?: false
        }
    }
}