package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class MediumRocketItem extends Item implements ProjectileItem {

    private final float damage;
    private final float radius;
    private final float explosionDamage;
    private final float fireProbability;
    private final int fireTime;
    public final MediumRocketEntity.Type type;
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
        return new MediumRocketEntity(ModEntities.MEDIUM_ROCKET.get(), pos.x(), pos.y(), pos.z(), level, damage, radius, explosionDamage, fireProbability, fireTime, type, sparedAmount, 15);
    }

    public static class MediumRocketDispenseBehavior extends ProjectileDispenseBehavior {
        public MediumRocketDispenseBehavior(Item item) {
            super(item);
        }

        @Override
        protected void playSound(BlockSource blockSource) {
            blockSource.level().playSound(null, blockSource.pos(), ModSounds.MEDIUM_ROCKET_FIRE.get(), SoundSource.BLOCKS, 2F, 1F);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return createProjectile(level, pos);
    }

    @Override
    public @NotNull DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder().power(6).build();
    }
}