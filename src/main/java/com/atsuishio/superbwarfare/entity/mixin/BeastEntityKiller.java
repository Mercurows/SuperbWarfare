package com.atsuishio.superbwarfare.entity.mixin;

import net.minecraft.world.entity.LivingEntity;

public interface BeastEntityKiller {

    static BeastEntityKiller getInstance(LivingEntity entity) {
        return (BeastEntityKiller) entity;
    }

    void sbw$kill();
}
