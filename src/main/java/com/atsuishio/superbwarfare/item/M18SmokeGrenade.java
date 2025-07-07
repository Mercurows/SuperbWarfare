package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.projectile.M18SmokeGrenadeEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class M18SmokeGrenade extends Item implements ProjectileItem {

    public M18SmokeGrenade() {
        super(new Properties().rarity(Rarity.UNCOMMON));
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull InteractionResultHolder<ItemStack> use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack stack = playerIn.getItemInHand(handIn);
        playerIn.startUsingItem(handIn);
        if (playerIn instanceof ServerPlayer serverPlayer) {
            serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.GRENADE_PULL.get(), SoundSource.PLAYERS, 1, 1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.SPEAR;
    }

    @Override
    @ParametersAreNonnullByDefault
    public void releaseUsing(ItemStack stack, Level worldIn, LivingEntity entityLiving, int timeLeft) {
        if (!worldIn.isClientSide && entityLiving instanceof Player player) {
            int usingTime = this.getUseDuration(stack, player) - timeLeft;
            if (usingTime > 3) {
                player.getCooldowns().addCooldown(stack.getItem(), 20);
                float power = Math.min(usingTime / 8f, 1.8f);

                M18SmokeGrenadeEntity grenade = new M18SmokeGrenadeEntity(player, worldIn, 80 - usingTime);
                grenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, power, 0);
                worldIn.addFreshEntity(grenade);

                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.GRENADE_THROW.get(), SoundSource.PLAYERS, 1, 1);
                }

                if (!player.isCreative()) {
                    stack.shrink(1);
                }
            }
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (!pLevel.isClientSide) {
            M18SmokeGrenadeEntity grenade = new M18SmokeGrenadeEntity(pLivingEntity, pLevel, 2);
            pLevel.addFreshEntity(grenade);

            if (pLivingEntity instanceof Player player) {
                player.getCooldowns().addCooldown(pStack.getItem(), 20);
            }

            if (pLivingEntity instanceof Player player && !player.isCreative()) {
                pStack.shrink(1);
            }
        }

        return super.finishUsingItem(pStack, pLevel, pLivingEntity);
    }

    @Override
    @ParametersAreNonnullByDefault
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 80;
    }

    public static class SmokeGrenadeDispenserBehavior extends ProjectileDispenseBehavior {
        public SmokeGrenadeDispenserBehavior() {
            super(ModItems.M18_SMOKE_GRENADE.get());
        }

        @Override
        protected void playSound(BlockSource blockSource) {
            blockSource.level().playSound(null, blockSource.pos(), ModSounds.GRENADE_THROW.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new M18SmokeGrenadeEntity(ModEntities.M18_SMOKE_GRENADE.get(), pos.x(), pos.y(), pos.z(), level);
    }
}

