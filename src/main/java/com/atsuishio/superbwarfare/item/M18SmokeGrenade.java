package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.entity.projectile.M18SmokeGrenadeEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import net.minecraft.core.BlockSource;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.AbstractProjectileDispenseBehavior;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

// TODO 物品图标，合成配方等资源

public class M18SmokeGrenade extends Item implements DispenserLaunchable {

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
            int usingTime = this.getUseDuration(stack) - timeLeft;
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
    public int getUseDuration(@NotNull ItemStack stack) {
        return 80;
    }

    @Override
    public DispenseItemBehavior getLaunchBehavior() {
        return new AbstractProjectileDispenseBehavior() {
            @Override
            @ParametersAreNonnullByDefault
            protected @NotNull Projectile getProjectile(Level pLevel, Position pPosition, ItemStack pStack) {
                return new M18SmokeGrenadeEntity(ModEntities.M18_SMOKE_GRENADE.get(), pPosition.x(), pPosition.y(), pPosition.z(), pLevel);
            }

            @Override
            protected void playSound(@NotNull BlockSource pSource) {
                pSource.getLevel().playSound(null, pSource.getPos(), ModSounds.GRENADE_THROW.get(), SoundSource.BLOCKS, 1, 1);
            }
        };
    }
}

