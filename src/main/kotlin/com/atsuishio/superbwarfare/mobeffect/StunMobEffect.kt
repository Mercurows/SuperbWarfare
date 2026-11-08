package com.atsuishio.superbwarfare.mobeffect

import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes

class StunMobEffect : MobEffect(MobEffectCategory.HARMFUL, 0xD9D9B5) {
    init {
        addAttributeModifier(
            Attributes.ATTACK_DAMAGE, "65591cf0-48bd-4499-b0df-b1a52699d63b",
            -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL
        )
        addAttributeModifier(
            Attributes.MOVEMENT_SPEED, "2c51769c-b9e8-4863-a5b0-452452b47fd7",
            -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL
        )
        addAttributeModifier(
            Attributes.FLYING_SPEED, "c1473384-032c-4bec-9b3a-53157c92676b",
            -1.0, AttributeModifier.Operation.MULTIPLY_TOTAL
        )
    }
}
