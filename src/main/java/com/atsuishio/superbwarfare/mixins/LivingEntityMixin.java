package com.atsuishio.superbwarfare.mixins;

import com.atsuishio.superbwarfare.entity.mixin.ICustomKnockback;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class LivingEntityMixin implements ICustomKnockback {

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
}