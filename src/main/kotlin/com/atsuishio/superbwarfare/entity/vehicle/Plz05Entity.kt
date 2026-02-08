package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.entity.buildControllers
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.atsuishio.superbwarfare.tools.angleTo
import com.atsuishio.superbwarfare.tools.toVec3
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import software.bernie.geckolib.animation.AnimatableManager.ControllerRegistrar

class Plz05Entity(type: EntityType<Plz05Entity>, world: Level) : ArtilleryEntity(type, world) {

    override fun getDamageModifier() = super.getDamageModifier()
        .custom { source, damage -> getSourceAngle(source, 0.3f) * damage }

    override fun baseTick() {
        super.baseTick()

        if (getNthEntity(turretControllerIndex) == null) {
            if (deltaMovement.horizontalDistanceSqr() > 0.007) {
                shootVec = getViewVec(this, 1f).toVector3f()
                if (shootVec.toVec3().angleTo(getShootVec("Main", 1f)) < 0.1) {
                    lockTurret = true
                }
            }
        } else {
            lockTurret = false
        }
    }

    override fun registerControllers(data: ControllerRegistrar) = buildControllers(data) {
        "shoot" {
            if (getShootAnimationTimer(1, 0) > 0) {
                thenPlay("animation.plz_05.shoot")
            } else {
                thenLoop("animation.plz_05.idle")
            }
        }
        "lockTurret"(10) {
            if (lockTurret) {
                thenPlay("animation.plz_05.lock_turret")
            } else {
                thenLoop("animation.plz_05.idle")
            }
        }
    }

    override fun canBind() = true
}
