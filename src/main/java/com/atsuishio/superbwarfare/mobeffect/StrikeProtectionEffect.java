package com.atsuishio.superbwarfare.mobeffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class StrikeProtectionEffect extends MobEffect {
    public StrikeProtectionEffect() {
        super(MobEffectCategory.BENEFICIAL, -12708330);
    }
}
