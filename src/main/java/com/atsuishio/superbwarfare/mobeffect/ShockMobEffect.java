package com.atsuishio.superbwarfare.mobeffect;

import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModMobEffects;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME)
public class ShockMobEffect extends MobEffect {

    public ShockMobEffect() {
        super(MobEffectCategory.HARMFUL, -256);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, ResourceLocation.withDefaultNamespace("effect.speed"), -10.0F, AttributeModifier.Operation.ADD_VALUE);
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        Entity attacker;
        if (!entity.getPersistentData().contains("TargetShockAttacker")) {
            attacker = null;
        } else {
            attacker = entity.level().getEntity(entity.getPersistentData().getInt("TargetShockAttacker"));
        }

        entity.hurt(ModDamageTypes.causeShockDamage(entity.level().registryAccess(), attacker), 2 + (1.25f * amplifier));
        entity.level().playSound(null, entity.getOnPos(), ModSounds.ELECTRIC.get(), SoundSource.PLAYERS, 1, 1);

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
        if (instance == null || !ModMobEffects.SHOCK.get().equals(instance.getEffect().value())) {
            return;
        }

        if (living instanceof Player) {
            if (!living.level().isClientSide()) {
                living.level().playSound(null, BlockPos.containing(living.getX(), living.getY(), living.getZ()), ModSounds.SHOCK.get(), SoundSource.HOSTILE, 1, 1);
            } else {
                living.level().playLocalSound(living.getX(), living.getY(), living.getZ(), ModSounds.SHOCK.get(), SoundSource.HOSTILE, 1, 1, false);
            }
        }

        living.hurt(ModDamageTypes.causeShockDamage(living.level().registryAccess(),
                event.getEffectSource()), 2 + (1.25f * instance.getAmplifier()));

        if (event.getEffectSource() instanceof LivingEntity source) {
            living.getPersistentData().putInt("TargetShockAttacker", source.getId());
        }
    }

    @SubscribeEvent
    public static void onEffectExpired(MobEffectEvent.Expired event) {
        LivingEntity living = event.getEntity();

        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) {
            return;
        }

        if (instance.getEffect().value().equals(ModMobEffects.SHOCK.value())) {
            living.getPersistentData().remove("TargetShockAttacker");
        }
    }

    @SubscribeEvent
    public static void onEffectRemoved(MobEffectEvent.Remove event) {
        LivingEntity living = event.getEntity();

        MobEffectInstance instance = event.getEffectInstance();
        if (instance == null) {
            return;
        }

        if (instance.getEffect().equals(ModMobEffects.SHOCK)) {
            living.getPersistentData().remove("TargetShockAttacker");
        }
    }

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        if (living.hasEffect(ModMobEffects.SHOCK)) {
            living.setXRot((float) Mth.nextDouble(RandomSource.create(), -23, -36));
            living.xRotO = living.getXRot();
        }
    }

    @SubscribeEvent
    public static void onEntityAttacked(LivingIncomingDamageEvent event) {
        if (event == null) return;

        DamageSource source = event.getSource();
        Entity entity = source.getDirectEntity();
        if (entity == null) return;

        if (entity instanceof LivingEntity living && living.hasEffect(ModMobEffects.SHOCK)) {
            event.setCanceled(true);
        }
    }
}
