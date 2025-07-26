package com.atsuishio.superbwarfare.mobeffect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
public class StrikeProtectionEffect extends MobEffect {
    public StrikeProtectionEffect() {
        super(MobEffectCategory.BENEFICIAL, -12708330);
    }
}
