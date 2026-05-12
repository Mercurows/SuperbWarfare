package com.atsuishio.superbwarfare.entity.mixin

import net.minecraft.sounds.SoundEvent
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.LivingEntity
import net.neoforged.neoforge.common.damagesource.DamageContainer
import java.util.*

@Suppress("FunctionName")
interface DamageAccess {
    fun `superbWarfare$getDeathSound`(): SoundEvent?

    fun `superbWarfare$getSoundVolume`(): Float

    fun `superbWarfare$playHurtSound`(pSource: DamageSource?)

    fun `superbWarfare$actuallyHurt`(pDamageSource: DamageSource?, pDamageAmount: Float)

    fun `superbWarfare$hurtHelmet`(pDamageSource: DamageSource?, pDamageAmount: Float)

    fun `superbWarfare$checkTotemDeathProtection`(pDamageSource: DamageSource?): Boolean

    fun `superbwarfare$getDamageContainers`(): Stack<DamageContainer>?

    companion object {
        fun of(living: LivingEntity): DamageAccess {
            return living as DamageAccess
        }
    }
}
