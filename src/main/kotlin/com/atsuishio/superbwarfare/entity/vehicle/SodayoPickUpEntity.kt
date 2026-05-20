package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class SodayoPickUpEntity(type: EntityType<SodayoPickUpEntity>, world: Level) : GeoVehicleEntity(type, world), BasicGeoVehicleEntity {
    override fun baseTick() {
        super.baseTick()
        if (decoyInputDown) {
            horn()
        }
    }
}
