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
        private val absent = object : ICustomKnockback {
            override fun `superbWarfare$setKnockbackStrength`(strength: Double) {}
            override fun `superbWarfare$resetKnockbackStrength`() {}
            override fun `superbWarfare$getKnockbackStrength`() = -1.0
        }

        /**
         * LivingEntity is only this interface when [com.atsuishio.superbwarfare.mixins.LivingEntityMixin]
         * applied. TACZ and other mods knock back vanilla mobs too, so a missing mixin must not crash the tick.
         */
        @JvmStatic
        fun getInstance(entity: LivingEntity): ICustomKnockback = entity as? ICustomKnockback ?: absent
    }
}
