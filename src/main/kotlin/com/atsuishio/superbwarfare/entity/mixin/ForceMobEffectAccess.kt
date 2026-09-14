package com.atsuishio.superbwarfare.entity.mixin

import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

@Suppress("FunctionName")
interface ForceMobEffectAccess {
    fun `superbWarfare$addEffectUnchecked`(
        instance: MobEffectInstance,
        source: Entity?
    ): Boolean

    companion object {
        fun of(living: LivingEntity): ForceMobEffectAccess {
            return living as ForceMobEffectAccess
        }
    }
}
