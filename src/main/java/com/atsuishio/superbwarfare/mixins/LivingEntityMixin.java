package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.DamageAccess;
import com.atsuishio.superbwarfare.entity.mixin.ForceMobEffectAccess;
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.tools.MinecraftUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Stack;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ICustomKnockback, DamageAccess, ForceMobEffectAccess {

    @Shadow
    @Final
    private Map<MobEffect, MobEffectInstance> activeEffects;

    @Shadow
    protected abstract void onEffectAdded(MobEffectInstance instance, @Nullable Entity source);

    @Shadow
    protected abstract void onEffectUpdated(MobEffectInstance instance, boolean forced, @Nullable Entity source);

    @Shadow
    @Nullable
    protected abstract SoundEvent getDeathSound();

    @Shadow
    protected abstract float getSoundVolume();

    @Shadow
    protected abstract void playHurtSound(DamageSource pSource);

    @Shadow
    protected abstract void actuallyHurt(DamageSource pDamageSource, float pDamageAmount);

    @Shadow
    protected abstract void hurtHelmet(DamageSource pDamageSource, float pDamageAmount);

    @Shadow
    protected abstract boolean checkTotemDeathProtection(DamageSource pDamageSource);

    @Shadow
    @Nullable
    protected Stack<DamageContainer> damageContainers;

    @Unique
    private double superbwarfare$knockbackStrength = -1;

    @Override
    public void superbWarfare$setKnockbackStrength(double strength) {
        this.superbwarfare$knockbackStrength = strength;
    }

    @Override
    public void superbWarfare$resetKnockbackStrength() {
        this.superbwarfare$knockbackStrength = -1;
    }

    @Override
    public double superbWarfare$getKnockbackStrength() {
        return this.superbwarfare$knockbackStrength;
    }

    @Inject(method = "setSprinting(Z)V", at = @At("HEAD"), cancellable = true)
    public void setSprinting(boolean pSprinting, CallbackInfo ci) {
        if (((LivingEntity) (Object) this) instanceof Player player && player.level().isClientSide) {
            if (pSprinting && ClientEventHandler.zoom) {
                ci.cancel();
            }
        }
    }

    @Override
    public SoundEvent superbWarfare$getDeathSound() {
        return this.getDeathSound();
    }

    @Override
    public float superbWarfare$getSoundVolume() {
        return this.getSoundVolume();
    }

    @Override
    public void superbWarfare$playHurtSound(DamageSource pSource) {
        this.playHurtSound(pSource);
    }

    @Override
    public void superbWarfare$actuallyHurt(DamageSource pDamageSource, float pDamageAmount) {
        this.actuallyHurt(pDamageSource, pDamageAmount);
    }

    @Override
    public void superbWarfare$hurtHelmet(DamageSource pDamageSource, float pDamageAmount) {
        this.hurtHelmet(pDamageSource, pDamageAmount);
    }

    @Override
    public boolean superbWarfare$checkTotemDeathProtection(DamageSource pDamageSource) {
        return this.checkTotemDeathProtection(pDamageSource);
    }

    @Override
    public @Nullable Stack<DamageContainer> superbwarfare$getDamageContainers() {
        return this.damageContainers;
    }

    @Override
    public boolean superbWarfare$addEffectUnchecked(
            @NotNull MobEffectInstance instance,
            @Nullable Entity source
    ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide) {
            return false;
        }

        MobEffectInstance old = this.activeEffects.get(instance.getEffect().value());
        MinecraftUtil.postEvent(
                new MobEffectEvent.Added(self, old, instance, source)
        );

        if (old == null) {
            this.activeEffects.put(instance.getEffect().value(), instance);
            this.onEffectAdded(instance, source);
            return true;
        }

        if (old.update(instance)) {
            this.onEffectUpdated(old, true, source);
            return true;
        }

        return false;
    }

    @Inject(method = "dismountVehicle", at = @At("RETURN"))
    private void dismountVehicle(Entity pVehicle, CallbackInfo ci) {
        if (pVehicle instanceof VehicleEntity vehicle) {
            vehicle.removeSeatIndexTag(((LivingEntity) (Object) this));
        }
    }

    @Shadow
    @Nullable
    public DamageSource lastDamageSource;

    @Shadow
    public long lastDamageStamp;

    @Inject(method = "playHurtSound", at = @At("HEAD"), cancellable = true)
    protected void playHurtSound(DamageSource pSource, CallbackInfo ci) {
        if (pSource.is(ModTags.DamageTypes.NO_HURT_EFFECT)) {
            ci.cancel();
        }
    }

    @Inject(method = "handleDamageEvent", at = @At("HEAD"), cancellable = true)
    public void handleDamageEvent(DamageSource pSource, CallbackInfo ci) {
        if (pSource.is(ModTags.DamageTypes.NO_HURT_EFFECT)) {
            ci.cancel();

            LivingEntity living = (LivingEntity) (Object) this;
            living.invulnerableTime = 0;
            living.hurtTime = 0;
            living.hurtDuration = 0;
            this.lastDamageSource = pSource;
            this.lastDamageStamp = living.level().getGameTime();
        }
    }
}
