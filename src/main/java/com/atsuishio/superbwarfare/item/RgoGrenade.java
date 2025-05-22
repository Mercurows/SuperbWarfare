package com.atsuishio.superbwarfare.item;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.projectile.RgoGrenadeEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.ParticleTool;
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
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

public class RgoGrenade extends Item implements ProjectileItem {

    public RgoGrenade() {
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
                    player.getCooldowns().addCooldown(stack.getItem(), 20);
                    float power = Math.min(usingTime / 8.0f, 1.8f);

                    RgoGrenadeEntity rgoGrenade = new RgoGrenadeEntity(player, worldIn, 80 - usingTime);
                    rgoGrenade.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, power, 0.0f);
                    worldIn.addFreshEntity(rgoGrenade);

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
            RgoGrenadeEntity rgoGrenade = new RgoGrenadeEntity(pLivingEntity, pLevel, 100);
            CustomExplosion explosion = new CustomExplosion(pLevel, null,
                    ModDamageTypes.causeProjectileBoomDamage(pLevel.registryAccess(), rgoGrenade, pLivingEntity), ExplosionConfig.RGO_GRENADE_EXPLOSION_DAMAGE.get(),
                    pLivingEntity.getX(), pLivingEntity.getY(), pLivingEntity.getZ(), ExplosionConfig.RPG_EXPLOSION_RADIUS.get(), ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1.25f);
            explosion.explode();
            EventHooks.onExplosionStart(pLevel, explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(pLevel, pLivingEntity.position());

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

    public static class RgoGrenadeDispenserBehavior extends ProjectileDispenseBehavior {
        public RgoGrenadeDispenserBehavior() {
            super(ModItems.RGO_GRENADE.get());
        }

        @Override
        protected void playSound(BlockSource blockSource) {
            blockSource.level().playSound(null, blockSource.pos(), ModSounds.GRENADE_THROW.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Override
    @ParametersAreNonnullByDefault
    public @NotNull Projectile asProjectile(Level level, Position pos, ItemStack stack, Direction direction) {
        return new RgoGrenadeEntity(ModEntities.RGO_GRENADE.get(), pos.x(), pos.y(), pos.z(), level);
    }
}

