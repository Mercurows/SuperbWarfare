package com.atsuishio.superbwarfare.mobeffect

import com.atsuishio.superbwarfare.Mod.Companion.loc
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.ai.attributes.AttributeModifier
import net.minecraft.world.entity.ai.attributes.Attributes

class StunMobEffect : MobEffect(MobEffectCategory.HARMFUL, 0xD9D9B5) {
    init {
        addAttributeModifier(
            Attributes.ATTACK_DAMAGE, loc("effect.stun.attack_damage"),
            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        )
        addAttributeModifier(
            Attributes.MOVEMENT_SPEED, loc("effect.stun.movement_speed"),
            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        )
        addAttributeModifier(
            Attributes.FLYING_SPEED, loc("effect.stun.flying_speed"),
            -1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
        )
    }
}
