package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.atsuishio.superbwarfare.tools.angleTo
import com.atsuishio.superbwarfare.tools.toVec3
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class Plz05Entity(type: EntityType<Plz05Entity>, world: Level) : ArtilleryEntity(type, world), BasicGeoVehicleEntity {

    val anim: VehicleAnimationInstance<Plz05Entity>? =
        if (world.isClientSide) VehicleAnimationInstance(this) else null
    override fun getAnimationInstance() = anim
    override fun getAnimation() = ANIM
    companion object {
        val ANIM = Mod.loc("animation/bedrock/vehicle/plz_05.animation.json")
    }

    private var wasLockTurret = false

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

        if (level().isClientSide) {
            val ctx = anim?.context ?: return
            if (lockTurret && !wasLockTurret) {
                ctx.playAnimation("animation.plz_05.lock_turret", AnimationPlayType.LOOP,
                    fadeInTicks = 40)
            } else if (!lockTurret && wasLockTurret) {
                ctx.stopAnimation("animation.plz_05.lock_turret",
                    fadeOutTicks = 80)
            }
            wasLockTurret = lockTurret
        }
    }

    override fun canBind() = true
}
