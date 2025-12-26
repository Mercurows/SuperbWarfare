package com.atsuishio.superbwarfare.mobeffect;

import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@net.minecraftforge.fml.common.Mod.EventBusSubscriber(bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.FORGE)
public class PhosphorusFireMobEffect extends MobEffect {

    public static final String TAG_PHOSPHORUS_FIRE_COUNT = "SbwPhosphorusFireCount";
    public static final String TAG_PHOSPHORUS_FIRE_ATTACKER = "SbwPhosphorusFireAttacker";

    public PhosphorusFireMobEffect() {
        super(MobEffectCategory.HARMFUL, 0xB1C1F2);
    }

    @Override
    public void applyEffectTick(LivingEntity living, int amplifier) {
        Entity attacker;
        if (!living.getPersistentData().contains(TAG_PHOSPHORUS_FIRE_ATTACKER)) {
            attacker = null;
        } else {
            attacker = living.level().getEntity(living.getPersistentData().getInt(TAG_PHOSPHORUS_FIRE_ATTACKER));
        }

        int fireCount = living.getPersistentData().getInt(TAG_PHOSPHORUS_FIRE_COUNT);
        int fireLevel = fireCount / 4;

        float damage = 1f + 0.5f * amplifier + Math.min((amplifier + 1) * 4.5f, fireLevel * (amplifier * 1.5f + 0.5f));

        DamageHandler.doDamage(living, ModDamageTypes.causePhosphorusFireDamage(living.level().registryAccess(), null, attacker), damage);
        living.invulnerableTime = 0;

        living.getPersistentData().putInt(TAG_PHOSPHORUS_FIRE_COUNT, fireCount + 1);
    }

    @Override
    public boolean isDurationEffectTick(int pDuration, int pAmplifier) {
        return pDuration % 10 == 0;
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity living = event.getEntity();

        MobEffectInstance instance = event.getEffectInstance();
        if (!instance.getEffect().equals(ModMobEffects.PHOSPHORUS_FIRE.get())) {
            return;
        }

        if (event.getEffectSource() instanceof LivingEntity source) {
            living.getPersistentData().putInt(TAG_PHOSPHORUS_FIRE_ATTACKER, source.getId());
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity living = event.getEntity();

        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) {
            return;
        }

        if (instance.getEffect().equals(ModMobEffects.PHOSPHORUS_FIRE.get())) {
            living.getPersistentData().remove(TAG_PHOSPHORUS_FIRE_ATTACKER);
            living.getPersistentData().remove(TAG_PHOSPHORUS_FIRE_COUNT);
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity living = event.getEntity();

        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) {
            return;
        }

        if (instance.getEffect().equals(ModMobEffects.PHOSPHORUS_FIRE.get())) {
            living.getPersistentData().remove(TAG_PHOSPHORUS_FIRE_ATTACKER);
            living.getPersistentData().remove(TAG_PHOSPHORUS_FIRE_COUNT);
        }
    }
}
