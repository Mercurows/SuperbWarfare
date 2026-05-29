package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
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

    override fun canBind() = true
}
