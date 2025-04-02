package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class MortarShell extends Item {

    public MortarShell() {
        super(new Properties());
    }

    public MortarShellEntity createShell(LivingEntity entity, Level level, ItemStack stack) {
        MortarShellEntity shellEntity = new MortarShellEntity(entity, level);
        shellEntity.setEffectsFromItem(stack);
        return shellEntity;
    }
}
