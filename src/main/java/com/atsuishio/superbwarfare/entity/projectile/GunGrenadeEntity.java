package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

public class GunGrenadeEntity extends FastThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GunGrenadeEntity(EntityType<? extends GunGrenadeEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public GunGrenadeEntity(@Nullable Entity entity, Level level, float damage, float explosionDamage, float explosionRadius) {
        super(ModEntities.GUN_GRENADE.get(), entity, level);
        this.noCulling = true;
        this.damage = damage;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.GRENADE_40MM.get();
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;

        if (this.getOwner() instanceof LivingEntity living) {
            if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
            }
        }

        DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), damage);

        if (entity instanceof LivingEntity) {
            entity.invulnerableTime = 0;
        }

        if (this.tickCount > 0) {
            if (this.level() instanceof ServerLevel) {
                ProjectileTool.causeCustomExplode(this,
                        ModDamageTypes.causeProjectileExplosionDamage(this.level().registryAccess(), this, this.getOwner()),
                        entity, this.explosionDamage, this.explosionRadius);
            }
        }

        this.discard();
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        BlockPos resultPos = blockHitResult.getBlockPos();
        BlockState state = this.level().getBlockState(resultPos);

        if (state.getBlock() instanceof BellBlock bell) {
            bell.attemptToRing(this.level(), resultPos, blockHitResult.getDirection());
        }
        if (this.level() instanceof ServerLevel) {
            ProjectileTool.causeCustomExplode(this,
                    ModDamageTypes.causeProjectileExplosionDamage(this.level().registryAccess(), this, this.getOwner()),
                    this, this.explosionDamage, this.explosionRadius);
        }
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        smallTrail();
        if (this.tickCount > 200 || this.isInWater()) {
            if (this.level() instanceof ServerLevel) {
                ProjectileTool.causeCustomExplode(this,
                        ModDamageTypes.causeProjectileExplosionDamage(this.level().registryAccess(), this, this.getOwner()),
                        this, this.explosionDamage, this.explosionRadius);
            }
            this.discard();
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
