package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class WaveforceTowerEntity(type: EntityType<WaveforceTowerEntity>, world: Level) : AutoAimableEntity(type, world), BasicGeoVehicleEntity {
    init {
        this.noCulling = true
    }

//    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
//        "barrelLight" {
//            thenLoop("animation.waveforce_tower.idle")
//        }
//    }
}
