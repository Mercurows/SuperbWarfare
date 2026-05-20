package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class LaserTowerEntity(type: EntityType<LaserTowerEntity>, world: Level) : AutoAimableEntity(type, world), BasicGeoVehicleEntity {
    init {
        this.noCulling = true
    }
}
