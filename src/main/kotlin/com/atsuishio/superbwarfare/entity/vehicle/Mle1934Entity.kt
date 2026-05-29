package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Mle1934Entity(type: EntityType<Mle1934Entity>, world: Level) : ArtilleryEntity(type, world), BasicGeoVehicleEntity {
    val anim: VehicleAnimationInstance<Mle1934Entity>? =
        if (world.isClientSide) VehicleAnimationInstance(this) else null
    override fun getAnimationInstance() = anim
    override fun getAnimation() = ANIM
    companion object {
        val ANIM = Mod.loc("animation/bedrock/vehicle/mle_1934.animation.json")
    }

    override fun canBind() = true
}
