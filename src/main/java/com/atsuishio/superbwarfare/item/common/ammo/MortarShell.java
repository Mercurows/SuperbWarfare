package com.atsuishio.superbwarfare.item.common.ammo;

import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

public class MortarShell extends Item implements ProjectileItem {

    public MortarShell() {
        super(new Properties().stacksTo(8));
    }

    public static MortarShellEntity createShell(@Nullable LivingEntity entity, Level level, ItemStack stack, float gravity, float damage, float explosionDamage, float explosionRadius) {
        MortarShellEntity shellEntity = new MortarShellEntity(entity, level, damage, explosionDamage, explosionRadius);
        shellEntity.setGravity(gravity);
        shellEntity.setEffectsFromItem(stack);
        shellEntity.setType(stack.is(ModItems.MORTAR_SHELL_WP.get()) ? MortarShellEntity.Type.WP : MortarShellEntity.Type.NORMAL);
        return shellEntity;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        var shell = new MortarShellEntity(ModEntities.MORTAR_SHELL.get(), pos.x(), pos.y(), pos.z(), level, 0.13f);
        shell.setEffectsFromItem(stack);
        return shell;
    }

    @Override
    public @NotNull DispenseConfig createDispenseConfig() {
        return DispenseConfig.builder()
                .power(0.5F)
                .build();
    }

    public static class MortarShellDispenseBehavior extends ProjectileDispenseBehavior {
        public MortarShellDispenseBehavior(Item item) {
            super(item);
        }

        @Override
        protected void playSound(BlockSource blockSource) {
            blockSource.level().playSound(null, blockSource.pos(), ModSounds.MORTAR_FIRE.get(), SoundSource.BLOCKS, 1F, 1F);
        }
    }

}
