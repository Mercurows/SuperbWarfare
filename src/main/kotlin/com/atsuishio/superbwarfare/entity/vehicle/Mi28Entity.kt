package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class Mi28Entity(type: EntityType<Mi28Entity>, world: Level) : VehicleEntity(type, world), BasicGeoVehicleEntity {
    val anim: VehicleAnimationInstance<Mi28Entity>? =
        if (world.isClientSide) VehicleAnimationInstance(this) else null
    override fun getAnimationInstance() = anim
    override fun getAnimation() = ANIM
    companion object {
        val ANIM = Mod.loc("animation/bedrock/vehicle/mi_28.animation.json")
    }
}
