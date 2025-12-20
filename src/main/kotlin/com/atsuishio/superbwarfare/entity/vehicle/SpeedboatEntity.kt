package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar

class SpeedboatEntity(type: EntityType<SpeedboatEntity>, world: Level) : GeoVehicleEntity(type, world) {

    override fun registerControllers(data: ControllerRegistrar) = buildControllers(data) {
        "machineGun" {
            if (getShootAnimationTimer(0, 0) > 0) {
                thenPlay("animation.speedboat.fire")
            } else {
                thenLoop("animation.speedboat.idle")
            }
        }
    }
}
