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

public class MediumRocketItem extends Item implements DispenserLaunchable {

    private final float damage;
    private final float radius;
    private final float explosionDamage;
    private final float fireProbability;
    private final int fireTime;
    private final MediumRocketEntity.Type type;
    private final int sparedAmount;

    public MediumRocketItem(float damage, float radius, float explosionDamage, float fireProbability, int fireTime, MediumRocketEntity.Type type, int sparedAmount) {
        super(new Properties());

        this.damage = damage;
        this.radius = radius;
        this.explosionDamage = explosionDamage;
        this.fireProbability = fireProbability;
        this.fireTime = fireTime;
        this.type = type;
        this.sparedAmount = sparedAmount;
    }

    public MediumRocketEntity createProjectile(Level level, Position pos) {
        return new MediumRocketEntity(ModEntities.MEDIUM_ROCKET.get(), pos.x(), pos.y(), pos.z(), level, damage, radius, explosionDamage, fireProbability, fireTime, type, sparedAmount);
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
                return createProjectile(pLevel, pPosition);
            }

            @Override
            protected void playSound(@NotNull BlockSource pSource) {
                pSource.getLevel().playSound(null, pSource.getPos(), ModSounds.MEDIUM_ROCKET_FIRE.get(), SoundSource.BLOCKS, 4.0F, 1.0F);
            }
        };
    }
}