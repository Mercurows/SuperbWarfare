package com.atsuishio.superbwarfare.entity.vehicle.base;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public interface LockTargetEntity {

    boolean setTarget(ItemStack stack);

    void resetTarget();

    void look(Vec3 pTarget);
}
