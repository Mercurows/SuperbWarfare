package com.atsuishio.superbwarfare.entity.mixin;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

import java.util.Stack;

public interface DamageAccess {

    static DamageAccess of(LivingEntity living) {
        return (DamageAccess) living;
    }

    SoundEvent superbWarfare$getDeathSound();

    float superbWarfare$getSoundVolume();

    void superbWarfare$playHurtSound(DamageSource pSource);

    void superbWarfare$actuallyHurt(DamageSource pDamageSource, float pDamageAmount);

    void superbWarfare$hurtHelmet(DamageSource pDamageSource, float pDamageAmount);

    boolean superbWarfare$checkTotemDeathProtection(DamageSource pDamageSource);

    Stack<DamageContainer> superbwarfare$getDamageContainers();
}
