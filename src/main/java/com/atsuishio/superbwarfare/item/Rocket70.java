package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.projectile.HeliRocketEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class Rocket70 extends Item implements ProjectileItem {

    public Rocket70() {
        super(new Properties());
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new HeliRocketEntity(ModEntities.HELI_ROCKET.get(), pos.x(), pos.y(), pos.z(), level);
    }

    // TODO 发射音效
    @Override
    public @NotNull DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder()
                .uncertainty(1)
                .power(4)
                .build();
    }
}