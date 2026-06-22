package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class TruckEntity(type: EntityType<TruckEntity>, world: Level) : VehicleEntity(type, world) {
    override fun baseTick() {
        super.baseTick()
        if (decoyInputDown) {
            horn()
        }
    }
}
