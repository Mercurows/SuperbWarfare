package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar

class Lav150Entity(type: EntityType<Lav150Entity>, world: Level) : GeoVehicleEntity(type, world) {

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.15f) * damage }

    override fun registerControllers(data: ControllerRegistrar) = buildControllers(data) {
        "cannon" {
            if (getShootAnimationTimer(0, 0) > 0) {
                thenPlay("animation.lav_150.fire")
            } else {
                thenLoop("animation.lav_150.idle")
            }
        }
        "machineGun" {
            if (getShootAnimationTimer(0, 1) > 0) {
                thenPlay("animation.lav_150.fire2")
            } else {
                thenLoop("animation.lav_150.idle2")
            }
        }
    }
}
