package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Lav150Entity(type: EntityType<Lav150Entity>, world: Level) : GeoVehicleEntity(type, world), BasicGeoVehicleEntity {
    val anim: VehicleAnimationInstance<Lav150Entity>? =
        if (world.isClientSide) VehicleAnimationInstance(this) else null

    override fun getAnimation() = ANIM

    companion object {
        val ANIM = Mod.loc("animation/bedrock/vehicle/lav_150.animation.json")
    }
}
