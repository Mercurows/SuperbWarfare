package com.atsuishio.superbwarfare.entity.mixin;

import net.minecraft.world.entity.LivingEntity;

/**
 * Codes Based On @TACZ
 */
public interface ICustomKnockback {

    static ICustomKnockback getInstance(LivingEntity entity) {
        return (ICustomKnockback) entity;
    }

    void superbWarfare$setKnockbackStrength(double strength);

    void superbWarfare$resetKnockbackStrength();

    double superbWarfare$getKnockbackStrength();
}
