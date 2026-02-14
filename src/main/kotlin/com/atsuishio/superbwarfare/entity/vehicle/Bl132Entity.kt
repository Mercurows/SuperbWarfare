package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar

class Bl132Entity(type: EntityType<Bl132Entity>, world: Level) : ArtilleryEntity(type, world) {

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.1f) * damage }

    override fun registerControllers(data: ControllerRegistrar) = buildControllers(data) {
        for (i in 1..4) {
            "fire$i" {
                if (barrelAnim.getOrElse(i) { 0 } > 0) {
                    thenPlay("animation.bl_132.fire_${5 - i}")
                } else {
                    thenLoop("animation.bl_132.idle")
                }
            }
        }
    }

    override fun canBind() = true
}
