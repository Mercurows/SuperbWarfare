package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class MortarShell extends Item implements ProjectileItem {

    public MortarShell() {
        super(new Properties());
    }

    public MortarShellEntity createShell(LivingEntity entity, Level level, ItemStack stack) {
        MortarShellEntity shellEntity = new MortarShellEntity(entity, level);
        shellEntity.setEffectsFromItem(stack);
        return shellEntity;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        var shell = new MortarShellEntity(ModEntities.MORTAR_SHELL.get(), pos.x(), pos.y(), pos.z(), level);
        shell.setEffectsFromItem(stack);
        return shell;
    }
}
