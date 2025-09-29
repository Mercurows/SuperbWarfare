package com.atsuishio.superbwarfare.mobeffect;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.GAME)
public class TraumaMobEffect extends MobEffect {

    public TraumaMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xF4ADB4);
    }

    @SubscribeEvent
    public static void onLivingHeal(LivingHealEvent event) {
        var entity = event.getEntity();
        var effect = entity.getEffect(ModMobEffects.TRAUMA);
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
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        var entity = event.getEntity();
        var effect = entity.getEffect(ModMobEffects.TRAUMA);
        if (effect == null) return;

        int amp = effect.getAmplifier() + 1;
        float amount = event.getAmount();
        event.setAmount(amount * (1 + amp * 0.15f));
    }
}
