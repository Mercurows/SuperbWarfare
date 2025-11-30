package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class LaserTowerEntity(type: EntityType<LaserTowerEntity>, world: Level) : AutoAimableEntity(type, world) {
    init {
        this.noCulling = true
    }
}
