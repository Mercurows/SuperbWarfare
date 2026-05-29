package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class WaveforceTowerEntity(type: EntityType<WaveforceTowerEntity>, world: Level) : AutoAimableEntity(type, world), BasicGeoVehicleEntity {
    init {
        this.noCulling = true
    }

    val anim: VehicleAnimationInstance<WaveforceTowerEntity>? =
        if (world.isClientSide) VehicleAnimationInstance(this) else null
    override fun getAnimationInstance() = anim
    override fun getAnimation() = ANIM
    companion object {
        val ANIM = Mod.loc("animation/bedrock/vehicle/waveforce_tower.animation.json")
    }
}
