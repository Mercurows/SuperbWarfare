package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

class Yx100Entity(type: EntityType<Yx100Entity>, world: Level) : VehicleEntity(type, world), BasicGeoVehicleEntity {
    val anim: VehicleAnimationInstance<Yx100Entity>? =
        if (world.isClientSide) VehicleAnimationInstance(this) else null

    override fun getAnimationInstance() = anim

    override fun getTrackAnimationLength() = 80
    override fun getTurretMaxHealth() = 200f
    override fun getWheelMaxHealth() = 200f
    override fun getEngineMaxHealth() = 300f

    override val customTurretMinPitch: Float
        get() = if (Mth.abs(turretYRot) > 135) ((Mth.abs(turretYRot) - 135) * 0.5f).coerceAtMost(5f) else 0f

    override fun getAnimation() = ANIM
    companion object {
        val ANIM = Mod.loc("animation/bedrock/vehicle/yx_100.animation.json")
    }
}
