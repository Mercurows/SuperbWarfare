package com.atsuishio.superbwarfare.entity.mixin;

import net.minecraft.world.entity.Entity;

public interface CustomStopRiding {

    static CustomStopRiding getInstance(Entity entity) {
        return (CustomStopRiding) entity;
    }

    void superbwarfare$stopRiding();
}
