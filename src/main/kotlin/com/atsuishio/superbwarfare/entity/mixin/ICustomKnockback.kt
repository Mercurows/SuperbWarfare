package com.atsuishio.superbwarfare.entity.mixin

import net.minecraft.world.entity.LivingEntity

/**
 * Codes Based On @TACZ
 */
@Suppress("FunctionName")
interface ICustomKnockback {
    fun `superbWarfare$setKnockbackStrength`(strength: Double)

    fun `superbWarfare$resetKnockbackStrength`()

    fun `superbWarfare$getKnockbackStrength`(): Double

    companion object {
        @JvmStatic
        fun getInstance(entity: LivingEntity): ICustomKnockback {
            return entity as ICustomKnockback
        }
    }
}
