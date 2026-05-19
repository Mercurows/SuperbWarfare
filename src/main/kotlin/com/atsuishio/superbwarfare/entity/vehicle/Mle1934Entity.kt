package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.GeckoArtilleryEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.core.animation.AnimatableManager

class Mle1934Entity(type: EntityType<Mle1934Entity>, world: Level) : GeckoArtilleryEntity(type, world) {

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.25f) * damage }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
        "fireLeft" {
            if (barrelAnim.getOrElse(1) { 0 } > 0) {
                thenPlay("animation.mle_1934.fire_left")
            } else {
                thenLoop("animation.mle_1934.idle")
            }
        }
        "fireRight" {
            if (barrelAnim.getOrElse(0) { 0 } > 0) {
                thenPlay("animation.mle_1934.fire_right")
            } else {
                thenLoop("animation.mle_1934.idle")
            }
        }
    }

    override fun canBind() = true
}
