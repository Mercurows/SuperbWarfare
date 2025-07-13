package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.DispenserLaunchable;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class MediumRocketAPItem extends Item implements DispenserLaunchable {

    public MediumRocketAPItem() {
        super(new Properties());
    }

    @Override
    public AbstractProjectileDispenseBehavior getLaunchBehavior() {
        return new AbstractProjectileDispenseBehavior() {

            @Override
            protected float getPower() {
                return 6F;
            }

            @Override
            @ParametersAreNonnullByDefault
            protected @NotNull Projectile getProjectile(Level pLevel, Position pPosition, ItemStack pStack) {
                return new MediumRocketEntity(ModEntities.MEDIUM_ROCKET.get(), pPosition.x(), pPosition.y(), pPosition.z(), pLevel, 500, 6, 100, 0, 0, true, false, false, 0);
            }

            @Override
            protected void playSound(BlockSource pSource) {
                pSource.getLevel().playSound(null, pSource.getPos(), ModSounds.MEDIUM_ROCKET_FIRE.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
            }
        };
    }
}