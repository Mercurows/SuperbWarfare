package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class WaveforceTowerEntity(type: EntityType<WaveforceTowerEntity>, world: Level) : AutoAimableEntity(type, world) {
    init {
        this.noCulling = true
    }
}
