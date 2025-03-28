package com.atsuishio.superbwarfare.headshot;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * Author: MrCrayfish
 */
public interface IHeadshotBox<T extends Entity> {

    @Nullable
    AABB getHeadshotBox(T entity);
}