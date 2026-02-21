package com.atsuishio.superbwarfare.inventory.menu

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType
import net.minecraftforge.common.extensions.IForgeMenuType

class SmallVehicleContainerMenu(id: Int, inventory: Inventory, entityId: Int) :
    AbstractVehicleContainerMenu(TYPE, id, inventory, entityId) {
    override fun getRows(): Int = 3

    override fun addVehicleInventory() {
        for (r in 0 until getRows()) {
            for (c in 0 until 9) {
                this.addSlot(VehicleSlot(this.vehicle, c + r * 9, 8 + c * 18, 18 + r * 18))
            }
        }
    }

    companion object {
        @JvmField
        val TYPE: MenuType<SmallVehicleContainerMenu> =
            IForgeMenuType.create { id, inventory, buf -> SmallVehicleContainerMenu(id, inventory, buf.readInt()) }
    }
}