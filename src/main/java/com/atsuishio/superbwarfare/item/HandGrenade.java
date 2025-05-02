package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.projectile.HandGrenadeEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class HandGrenade extends Item implements ProjectileItem {

    public HandGrenade() {
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
        if (!worldIn.isClientSide) {
            if (entityLiving instanceof Player player) {
                int usingTime = this.getUseDuration(stack, player) - timeLeft;
                if (usingTime > 3) {
                    player.getCooldowns().addCooldown(stack.getItem(), 25);
                    float power = Math.min(usingTime / 10.0f, 1.5f);

                    HandGrenadeEntity handGrenade = new HandGrenadeEntity(player, worldIn, 100 - usingTime);
                    handGrenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, power, 0.0f);
                    worldIn.addFreshEntity(handGrenade);

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.level().playSound(null, serverPlayer.getOnPos(), ModSounds.GRENADE_THROW.get(), SoundSource.PLAYERS, 1, 1);
                    }

                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                }
            }
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull ItemStack finishUsingItem(ItemStack pStack, Level pLevel, LivingEntity pLivingEntity) {
        if (!pLevel.isClientSide) {
            HandGrenadeEntity handGrenade = new HandGrenadeEntity(pLivingEntity, pLevel, 100);

            CustomExplosion explosion = new CustomExplosion(pLevel, null,
                    ModDamageTypes.causeProjectileBoomDamage(pLevel.registryAccess(), handGrenade, pLivingEntity), ExplosionConfig.M67_GRENADE_EXPLOSION_DAMAGE.get(),
                    pLivingEntity.getX(), pLivingEntity.getY(), pLivingEntity.getZ(), ExplosionConfig.M67_GRENADE_EXPLOSION_RADIUS.get(), ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1.25f);
            explosion.explode();
            EventHooks.onExplosionStart(pLevel, explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(pLevel, pLivingEntity.position());

            if (pLivingEntity instanceof Player player) {
                player.getCooldowns().addCooldown(pStack.getItem(), 25);
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
        return 100;
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new HandGrenadeEntity(ModEntities.HAND_GRENADE.get(), pos.x(), pos.y(), pos.z(), level);
    }
}

