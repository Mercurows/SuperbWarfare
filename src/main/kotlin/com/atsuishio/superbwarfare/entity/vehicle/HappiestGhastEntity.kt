package com.atsuishio.superbwarfare.entity.vehicle

import com.atsuishio.superbwarfare.client.animation.AnimationPlayType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level

open class HappiestGhastEntity(type: EntityType<HappiestGhastEntity>, world: Level) : VehicleEntity(type, world) {
    override fun baseTick() {
        super.baseTick()

        if (level().isClientSide) {
            val ctx = anim?.context ?: return
            if (tickCount > 1) {
                ctx.playAnimation("animation.ghast.wave", AnimationPlayType.LOOP, fadeInTicks = 20)
            }
        }
    }
}
