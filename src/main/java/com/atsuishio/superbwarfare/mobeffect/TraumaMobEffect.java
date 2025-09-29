package com.atsuishio.superbwarfare.mobeffect;

import com.atsuishio.superbwarfare.init.ModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
public class TraumaMobEffect extends MobEffect {

    public TraumaMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xF4ADB4);
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        var entity = event.getEntity();
        var effect = entity.getEffect(ModMobEffects.TRAUMA.get());
        if (effect == null) return;

        int amp = effect.getAmplifier() + 1;
        if (amp >= 10) {
            event.setCanceled(true);
            return;
        }

        float amount = event.getAmount();
        event.setAmount(amount * (1 - amp * 0.1f));
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        var entity = event.getEntity();
        var effect = entity.getEffect(ModMobEffects.TRAUMA.get());
        if (effect == null) return;

        int amp = effect.getAmplifier() + 1;
        float amount = event.getAmount();
        event.setAmount(amount * (1 + amp * 0.05f));
    }
}
