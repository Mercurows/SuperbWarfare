package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.client.animation.entity.VehicleAnimationInstance
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class HappiestGhastEntity(type: EntityType<HappiestGhastEntity>, world: Level) : VehicleEntity(type, world), BasicGeoVehicleEntity {
    val anim: VehicleAnimationInstance<HappiestGhastEntity>? =
        if (world.isClientSide) VehicleAnimationInstance(this) else null
    override fun getAnimationInstance() = anim
    override fun getAnimation() = ANIM
    companion object {
        val ANIM = Mod.loc("animation/bedrock/vehicle/happiest_ghast.animation.json")
    }

    override fun baseTick() {
        super.baseTick()

        if (level().isClientSide) {
            val ctx = anim?.context ?: return
            if (tickCount > 1) {
                ctx.playAnimation("animation.ghast.wave", AnimationPlayType.LOOP,
                    fadeInTicks = 20)
            }
        }
    }
}
