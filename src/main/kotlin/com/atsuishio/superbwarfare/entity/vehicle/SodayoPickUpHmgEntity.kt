package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.animation.AnimatableManager

class SodayoPickUpHmgEntity(type: EntityType<SodayoPickUpHmgEntity>, world: Level) : GeoVehicleEntity(type, world) {

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.25f) * damage }

    override fun baseTick() {
        super.baseTick()
        if (decoyInputDown) {
            horn()
        }
    }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
        "machineGun" {
            if (getShootAnimationTimer(2, 0) > 0) {
                thenPlay("animation.hmg.fire")
            } else {
                thenLoop("animation.hmg.idle")
            }
        }
    }
}
