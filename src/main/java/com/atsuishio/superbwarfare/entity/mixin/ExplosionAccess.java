package com.atsuishio.superbwarfare.entity.mixin;

import net.minecraft.world.level.Explosion;

public interface ExplosionAccess {

    static ExplosionAccess of(Explosion explosion) {
        return (ExplosionAccess) explosion;
    }

    float superbwarfare$getRadius();
}
