package com.atsuishio.superbwarfare.mobeffect;

import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class BurnMobEffect extends MobEffect {

    public BurnMobEffect() {
        super(MobEffectCategory.HARMFUL, -12708330);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Entity attacker;
        if (!entity.getPersistentData().contains("BurnAttacker")) {
            attacker = null;
        } else {
            attacker = entity.level().getEntity(entity.getPersistentData().getInt("BurnAttacker"));
        }

        entity.hurt(ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), attacker), 0.6f + (0.3f * amplifier));
        entity.invulnerableTime = 0;

        if (attacker instanceof ServerPlayer player) {
            player.level().playSound(null, player.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);
            PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
        }
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 20 == 0;
    }

    @SubscribeEvent
    public static void onEffectAdded(MobEffectEvent.Added event) {
        LivingEntity living = event.getEntity();

        MobEffectInstance instance = event.getEffectInstance();
        if (instance != null && !instance.getEffect().value().equals(ModMobEffects.BURN.value())) {
            return;
        }

        float amount = 0.6f + (0.3f * (instance == null ? 0 : instance.getAmplifier()));

        living.hurt(new DamageSource(living.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.IN_FIRE),
                event.getEffectSource()), amount);
        living.invulnerableTime = 0;

        if (event.getEffectSource() instanceof LivingEntity source) {
            living.getPersistentData().putInt("BurnAttacker", source.getId());
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity living = event.getEntity();

        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) {
            return;
        }

        if (instance.getEffect().equals(ModMobEffects.BURN)) {
            living.getPersistentData().remove("BurnAttacker");
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity living = event.getEntity();

        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) {
            return;
        }

        if (instance.getEffect().equals(ModMobEffects.BURN)) {
            living.getPersistentData().remove("BurnAttacker");
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Post event) {
        var entity = event.getEntity();
        if (!(entity instanceof LivingEntity living)) return;

        if (living.hasEffect(ModMobEffects.BURN)) {
            living.setRemainingFireTicks(2);
        }

        if (living.isInWater()) {
            living.removeEffect(ModMobEffects.BURN);
        }
    }
}
