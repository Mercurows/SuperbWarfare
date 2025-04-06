package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class SwarmDroneEntity extends FastThrowableProjectile implements GeoEntity {

    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(SwarmDroneEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Float> TARGET_X = SynchedEntityData.defineId(SwarmDroneEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TARGET_Y = SynchedEntityData.defineId(SwarmDroneEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TARGET_Z = SynchedEntityData.defineId(SwarmDroneEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float explosionDamage = 125f;
    private float explosionRadius = 6f;

    private float randomFloat;
    private int guide_type = 0;

    public SwarmDroneEntity(EntityType<? extends SwarmDroneEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public SwarmDroneEntity(LivingEntity entity, Level level, float explosionDamage, float explosionRadius) {
        super(ModEntities.SWARM_DRONE.get(), entity, level);
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.DRONE.get();
    }

    public void setTargetUuid(String uuid) {
        this.entityData.set(TARGET_UUID, uuid);
    }

    public void setGuideType(int guideType) {
        this.guide_type = guideType;
    }

    public void setTargetVec(Vec3 targetPos) {
        this.entityData.set(TARGET_X, (float) targetPos.x);
        this.entityData.set(TARGET_Y, (float) targetPos.y);
        this.entityData.set(TARGET_Z, (float) targetPos.z);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);

        builder.define(TARGET_UUID, "none")
                .define(TARGET_X, 0f)
                .define(TARGET_Y, 0f)
                .define(TARGET_Z, 0f);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        if (this.level() instanceof ServerLevel) {
            causeMissileExplode(this,
                    ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), this, this.getOwner()),
                    this, this.explosionDamage, this.explosionRadius);
        }
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel) {
            causeMissileExplode(this,
                    ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), this, this.getOwner()),
                    this, this.explosionDamage, this.explosionRadius);
        }
    }

    @Override
    public void tick() {
        super.tick();

        Entity entity = EntityFindUtil.findEntity(this.level(), entityData.get(TARGET_UUID));

        if (this.tickCount == 1) {
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, this.xo, this.yo, this.zo, 15, 0.8, 0.8, 0.8, 0.01, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.xo, this.yo, this.zo, 10, 0.8, 0.8, 0.8, 0.01, true);
            }
        }

        if (tickCount > 10 && this.getOwner() != null) {
            Entity shooter = this.getOwner();
            Vec3 targetPos;

            if (guide_type == 0 && entity != null) {
                targetPos = entity.getEyePosition();
            } else {
                targetPos = new Vec3(this.entityData.get(TARGET_X), this.entityData.get(TARGET_Y), this.entityData.get(TARGET_Z));
            }

            if (tickCount % 5 == 0) {
                randomFloat = random.nextFloat();
            }

            double dis = position().distanceTo(targetPos);
            double disShooter = shooter.position().distanceTo(targetPos);
            double randomPos = Mth.sin(0.25f * (tickCount + randomFloat)) * randomFloat * (dis / disShooter);

            Vec3 toVec = this.position().vectorTo(targetPos).normalize().add(new Vec3(randomPos, 0, randomPos));
            setDeltaMovement(getDeltaMovement().add(toVec.scale(0.5)));
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.85, 0.85, 0.85));

            if (dis < 0.5) {
                if (this.level() instanceof ServerLevel) {
                    causeMissileExplode(this,
                            ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), this, this.getOwner()),
                            this, this.explosionDamage, this.explosionRadius);
                }
                this.discard();
            }
        }

        if (this.tickCount > 300 || this.isInWater()) {
            if (this.level() instanceof ServerLevel) {
                causeMissileExplode(this,
                        ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), this, this.getOwner()),
                        this, this.explosionDamage, this.explosionRadius);
            }
            this.discard();
        }
    }

    @Override
    protected void updateRotation() {
    }

    public static void causeMissileExplode(ThrowableItemProjectile projectile, @Nullable DamageSource source, Entity target, float damage, float radius) {
        CustomExplosion explosion = new CustomExplosion(projectile.level(), projectile, source, damage,
                target.getX(), target.getEyeY(), target.getZ(), radius, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).setDamageMultiplier(1.25f);
        explosion.explode();
        EventHooks.onExplosionStart(projectile.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnMediumExplosionParticles(projectile.level(), projectile.position());
        projectile.discard();
    }

    private PlayState movementPredicate(AnimationState<SwarmDroneEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sd.fly"));
    }

    @Override
    protected double getDefaultGravity() {
        return tickCount > 10 ? 0 : 0.1f;
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
    public boolean shouldSyncMotion() {
        return true;
    }
}
