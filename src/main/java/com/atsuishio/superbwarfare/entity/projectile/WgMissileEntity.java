package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;

public class WgMissileEntity extends FastThrowableProjectile implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float damage = 250f;
    private float explosionDamage = 200f;
    private float explosionRadius = 10f;

    public WgMissileEntity(EntityType<? extends WgMissileEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
    }

    public WgMissileEntity(LivingEntity entity, Level level, float damage, float explosionDamage, float explosionRadius) {
        super(ModEntities.WG_MISSILE.get(), entity, level);
        this.damage = damage;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.WIRE_GUIDE_MISSILE.get();
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity entity = result.getEntity();
        if (this.tickCount < 1) return;
        if (entity == this.getOwner() || entity == this.getVehicle()) return;

        if (this.getOwner() instanceof LivingEntity living) {
            if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
            }
        }

        entity.hurt(ModDamageTypes.causeCannonFireDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

        if (entity instanceof LivingEntity) {
            entity.invulnerableTime = 0;
        }

        if (this.tickCount > 2) {
            if (this.level() instanceof ServerLevel) {
                causeMissileExplode(this,
                        ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(), this, this.getOwner()),
                        entity, this.explosionDamage, this.explosionRadius);
            }
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

        if (this.tickCount == 1) {
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, this.xo, this.yo, this.zo, 15, 0.8, 0.8, 0.8, 0.01, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.xo, this.yo, this.zo, 10, 0.8, 0.8, 0.8, 0.01, true);
            }
        }
        if (this.tickCount > 2) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.03, 1.03, 1.03));

            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.SMOKE, this.xo, this.yo, this.zo, 1, 0, 0, 0, 0, true);
            }
        }

        if (tickCount > 5 && this.getOwner() != null && getOwner().getVehicle() instanceof Bmp2Entity bmp2) {
            Entity shooter = this.getOwner();
            Vec3 lookVec = bmp2.getBarrelVector(1).normalize();
            Vec3 toVec = shooter.getEyePosition().vectorTo(this.getEyePosition()).normalize();
            Vec3 addVec = lookVec.add(toVec.scale(-0.85)).normalize();
            double angle = Mth.abs((float) VectorTool.calculateAngle(lookVec, toVec));
            setDeltaMovement(getDeltaMovement().add(addVec.scale(Math.min(0.1 + 0.15 * angle + distanceTo(getOwner()) * 0.003, tickCount < 15 ? 0.04 : 0.4))));

            // 控制速度
            if (this.getDeltaMovement().length() < 2.8) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(1.06, 1.06, 1.06));
            }

            if (this.getDeltaMovement().length() > 3) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.9, 0.9, 0.9));
            }

            this.setDeltaMovement(this.getDeltaMovement().multiply(0.96, 0.96, 0.96));
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

    public static void causeMissileExplode(ThrowableItemProjectile projectile, @Nullable DamageSource source, Entity target, float damage, float radius) {
        CustomExplosion explosion = new CustomExplosion(projectile.level(), projectile, source, damage,
                target.getX(), target.getEyeY(), target.getZ(), radius, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP).setDamageMultiplier(1.25f);
        explosion.explode();
        EventHooks.onExplosionStart(projectile.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnMediumExplosionParticles(projectile.level(), projectile.position());
        projectile.discard();
    }

    private PlayState movementPredicate(AnimationState<WgMissileEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.jvm.idle"));
    }

    @Override
    protected double getDefaultGravity() {
        return 0;
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
