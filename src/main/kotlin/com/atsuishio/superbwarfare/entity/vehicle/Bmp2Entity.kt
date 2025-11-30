package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.core.animation.AnimatableManager

class Bmp2Entity(type: EntityType<Bmp2Entity>, world: Level) : GeoVehicleEntity(type, world) {

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.4f) * damage }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
        add("cannon") {
            if (getShootAnimationTimer(0, 0) > 0) {
                thenPlay("animation.lav_150.fire")
            } else {
                thenLoop("animation.lav_150.idle")
            }
        }
        add("machineGun") {
            if (getShootAnimationTimer(0, 1) > 0) {
                thenPlay("animation.lav_150.fire2")
            } else {
                thenLoop("animation.lav_150.idle2")
            }
        }
    }
}
