package com.atsuishio.superbwarfare.entity.vehicle.base;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

public interface RemoteControllableTurret {

    boolean setTarget(ItemStack stack);

    void resetTarget();

    void look(Vec3 pTarget);

    boolean canRemoteFire();

    void remoteFire(@Nullable Player player);
}
