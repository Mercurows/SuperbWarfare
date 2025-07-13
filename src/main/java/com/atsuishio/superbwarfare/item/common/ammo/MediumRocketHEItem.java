package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class MediumRocketHEItem extends Item implements ProjectileItem {

    public MediumRocketHEItem() {
        super(new Properties());
    }

    public static class MediumRocketDispenseBehavior extends ProjectileDispenseBehavior {
        public MediumRocketDispenseBehavior() {
            super(ModItems.MEDIUM_ROCKET_HE.get());
        }

        @Override
        protected void playSound(net.minecraft.core.dispenser.BlockSource blockSource) {
            blockSource.level().playSound(null, blockSource.pos(), ModSounds.SMALL_ROCKET_FIRE_3P.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new MediumRocketEntity(ModEntities.MEDIUM_ROCKET.get(), pos.x(), pos.y(), pos.z(), level, 200, 12, 200, 0.2f, 40, false, true, false, 0);
    }

    @Override
    public @NotNull DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder().power(6).build();
    }
}