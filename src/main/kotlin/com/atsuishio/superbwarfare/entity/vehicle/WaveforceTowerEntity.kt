package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar

class WaveforceTowerEntity(type: EntityType<WaveforceTowerEntity>, world: Level) : AutoAimableEntity(type, world) {
    init {
        this.noCulling = true
    }

    override fun registerControllers(data: ControllerRegistrar) = buildControllers(data) {
        "barrelLight" {
            thenLoop("animation.waveforce_tower.idle")
        }
    }
}
