package com.atsuishio.superbwarfare.inventory.handler

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.core.NonNullList
import net.minecraft.world.item.ItemStack
import net.minecraftforge.items.ItemStackHandler

open class VehicleContainerHandler(size: Int, val vehicle: VehicleEntity) : ItemStackHandler(size) {
    override fun onContentsChanged(slot: Int) {
        this.vehicle.setChanged()
    }

    override fun isItemValid(slot: Int, stack: ItemStack): Boolean {
        return this.vehicle.canPlaceItem(slot, stack)
    }

    open fun clear() {
        this.stacks.clear()
    }

    fun getItems(): NonNullList<ItemStack> = this.stacks

    fun setItems(list: NonNullList<ItemStack>) {
        this.stacks = list
    }
}