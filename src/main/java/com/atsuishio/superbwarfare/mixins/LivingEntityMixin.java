package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.DamageAccess;
import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.Stack;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements ICustomKnockback, DamageAccess {

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

    @Inject(method = "dismountVehicle", at = @At("RETURN"))
    private void dismountVehicle(Entity pVehicle, CallbackInfo ci) {
        if (pVehicle instanceof VehicleEntity vehicle) {
            vehicle.removeSeatIndexTag(((LivingEntity) (Object) this));
        }
    }
}