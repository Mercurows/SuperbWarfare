package com.atsuishio.superbwarfare.inventory.menu

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.MenuType
import net.minecraftforge.common.extensions.IForgeMenuType

class LargeVehicleContainerMenu(id: Int, inventory: Inventory, entityId: Int) :
    AbstractVehicleContainerMenu(TYPE, id, inventory, entityId) {
    override fun getRows(): Int = 6

    override fun addVehicleInventory() {
        for (r in 0 until getRows()) {
            for (c in 0 until 13) {
                this.addSlot(VehicleSlot(this.vehicle, c + r * 13, 8 + c * 18 - 36, 18 + r * 18))
            }
        }
    }

    companion object {
        @JvmField
        val TYPE: MenuType<LargeVehicleContainerMenu> =
            IForgeMenuType.create { id, inventory, buf -> LargeVehicleContainerMenu(id, inventory, buf.readInt()) }
    }
}