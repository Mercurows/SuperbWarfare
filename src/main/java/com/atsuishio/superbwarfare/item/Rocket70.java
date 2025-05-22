package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.projectile.HeliRocketEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
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

public class Rocket70 extends Item implements ProjectileItem {

    public Rocket70() {
        super(new Properties());
    }

    public static class Rocket70DispenseBehavior extends ProjectileDispenseBehavior {
        public Rocket70DispenseBehavior() {
            super(ModItems.ROCKET_70.get());
        }

        @Override
        protected void playSound(BlockSource blockSource) {
            blockSource.level().playSound(null, blockSource.pos(), ModSounds.HELICOPTER_ROCKET_FIRE_3P.get(), SoundSource.BLOCKS, 2.0F, 1.0F);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new HeliRocketEntity(ModEntities.HELI_ROCKET.get(), pos.x(), pos.y(), pos.z(), level);
    }

    @Override
    public @NotNull DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder()
                .uncertainty(1)
                .power(4)
                .build();
    }
}