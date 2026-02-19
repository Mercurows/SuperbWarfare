package com.atsuishio.superbwarfare.inventory.handler

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraftforge.items.ItemStackHandler

open class VehicleContainerHandler(size: Int, val vehicle: VehicleEntity) : ItemStackHandler(size) {

}