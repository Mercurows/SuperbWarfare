package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.tools.DamageHandler;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class RpgRocketStandardEntity extends FastThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public RpgRocketStandardEntity(EntityType<? extends RpgRocketStandardEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.durability = 50;
        this.damage = 340f;
        this.explosionDamage = 80f;
        this.explosionRadius = 5;
        this.gravity = 0.015f;
    }

    public RpgRocketStandardEntity(EntityType<? extends ThrowableItemProjectile> pEntityType, double pX, double pY, double pZ, Level pLevel, float damage, float explosionDamage, float explosionRadius) {
        super(pEntityType, pX, pY, pZ, pLevel);
        this.noCulling = true;
        this.durability = 50;
        this.damage = damage;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
        this.gravity = 0.015f;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.RPG_ROCKET_STANDARD.get();
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel) {
            BlockPos resultPos = blockHitResult.getBlockPos();
            float hardness = this.level().getBlockState(resultPos).getBlock().defaultDestroyTime();
            if (hardness != -1) {
                if (ExplosionConfig.EXPLOSION_DESTROY.get()) {
                    if (firstHit) {
                        causeExplode(blockHitResult.getLocation());
                        firstHit = false;
                        Mod.queueServerWork(3, this::discard);
                    }
                    if (ExplosionConfig.EXTRA_EXPLOSION_EFFECT.get()) {
                        this.level().destroyBlock(resultPos, true);
                    }
                }
            } else {
                causeExplode(blockHitResult.getLocation());
                this.discard();
            }
            if (!ExplosionConfig.EXPLOSION_DESTROY.get()) {
                causeExplode(blockHitResult.getLocation());
                this.discard();
            }
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;
        if (this.level() instanceof ServerLevel) {
            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            causeExplode(result.getLocation());
            this.discard();
        }
    }

    @Override
    public void tick() {
        super.tick();
        mediumTrail();

        if (this.tickCount == 3) {
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, this.xo, this.yo, this.zo, 15, 0.8, 0.8, 0.8, 0.01, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.xo, this.yo, this.zo, 10, 0.8, 0.8, 0.8, 0.01, true);
            }
        }
        if (this.tickCount > 2) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.03, 1.03, 1.03));
        }
        destroyBlock();
    }

    private PlayState movementPredicate(AnimationState<RpgRocketStandardEntity> event) {
        if (getDeltaMovement().lengthSqr() > 0) {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.idle"));
        } else {
            return event.setAndContinue(RawAnimation.begin().thenLoop("animation.rpg.none"));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "movement", 0, this::movementPredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public @NotNull SoundEvent getSound() {
        return ModSounds.ROCKET_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.2f;
    }
}
