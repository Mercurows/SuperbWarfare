package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.entity.mixin.ForceMobEffectAccess
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player

fun LivingEntity?.forceApplyEffect(
    instance: MobEffectInstance,
    source: Entity? = null
): Boolean {
    if (this == null) {
        return false
    }

    if (!MiscConfig.FORCE_MOB_EFFECT_MODE.get()) {
        return this.addEffect(instance, source)
    }

    if (this.level().isClientSide || this.isDeadOrDying) {
        return false
    }

    if (this is Player && (this.isCreative || this.isSpectator)) {
        return false
    }

    return ForceMobEffectAccess.of(this)
        .`superbWarfare$addEffectUnchecked`(instance, source)
}
