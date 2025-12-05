package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.core.animation.AnimatableManager

class Plz05Entity(type: EntityType<Plz05Entity>, world: Level) : ArtilleryEntity(type, world) {

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.3f) * damage }

    override fun baseTick() {
        super.baseTick()
        if (getNthEntity(turretControllerIndex) == null && deltaMovement.horizontalDistanceSqr() > 0.007) {
            shootVec = getForwardDirection()
        }
    }

    override fun registerControllers(data: AnimatableManager.ControllerRegistrar) = buildControllers(data) {
        add("shoot") {
            if (getShootAnimationTimer(1, 0) > 0) {
                thenPlay("animation.plz_05.shoot")
            } else {
                thenLoop("animation.plz_05.idle")
            }
        }
    }

    override fun canBind() = true
}
