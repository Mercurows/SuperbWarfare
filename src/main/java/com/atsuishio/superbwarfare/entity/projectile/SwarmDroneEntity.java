package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;

public class SwarmDroneEntity extends MissileProjectile implements GeoEntity, ExplosiveProjectile {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public float targetX;
    public float targetY;
    public float targetZ;
    public float randomFloat;
    public int guideType = 0;

    public SwarmDroneEntity(EntityType<? extends SwarmDroneEntity> type, Level level) {
        super(type, level);
        this.noCulling = true;
        this.explosionDamage = 80f;
        this.explosionRadius = 5f;
        this.gravity = 0.1f;
        randomFloat = random.nextFloat();
    }

    public SwarmDroneEntity(LivingEntity entity, Level level, float explosionDamage, float explosionRadius) {
        super(ModEntities.SWARM_DRONE.get(), entity, level);
        this.noCulling = true;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
        this.gravity = 0.1f;
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.DRONE.get();
    }

    public void setGuideType(int guideType) {
        this.guideType = guideType;
    }

    public void setTargetVec(Vec3 targetPos) {
        if (targetPos != null) {
            this.targetX = (float) targetPos.x;
            this.targetY = (float) targetPos.y;
            this.targetZ = (float) targetPos.z;
        }
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (entity instanceof SwarmDroneEntity) {
            return;
        }
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;
        if (this.getOwner() instanceof LivingEntity living) {
            if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
            }
        }
        if (this.level() instanceof ServerLevel) {
            causeMissileExplode(ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, this.getOwner()), this.explosionDamage, this.explosionRadius);
        }
    }

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        if (this.level() instanceof ServerLevel) {
            causeMissileExplode(ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, this.getOwner()), this.explosionDamage, this.explosionRadius);
        }
    }

    @Override
    public void tick() {
        super.tick();
        Entity entity = EntityFindUtil.findEntity(this.level(), entityData.get(TARGET_UUID));
        List<Entity> decoy = SeekTool.seekLivingEntities(this, 32, 90);

        for (var e : decoy) {
            if (e.getType().is(ModTags.EntityTypes.DECOY) && !this.distracted) {
                this.entityData.set(TARGET_UUID, e.getStringUUID());
                this.distracted = true;
                break;
            }
        }

        if (this.tickCount == 1) {
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, this.xo, this.yo, this.zo, 15, 0.8, 0.8, 0.8, 0.01, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.xo, this.yo, this.zo, 10, 0.8, 0.8, 0.8, 0.01, true);
            }
        }

        if (tickCount > 10 && this.getOwner() != null) {
            Entity shooter = this.getOwner();
            Vec3 targetPos;

            if (guideType == 0 && entity != null) {
                Vec3 targetVec = new Vec3(entity.getDeltaMovement().x, 0, entity.getDeltaMovement().z);
                targetPos = entity.getEyePosition().add(targetVec);
            } else {
                targetPos = new Vec3(targetX, targetY, targetZ);
            }

            if (tickCount %5 == 0) {
                randomFloat = 2 * (random.nextFloat() - 0.5f);
            }

            double dis = position().vectorTo(shooter.position()).horizontalDistance();
            double dis2 = position().distanceToSqr(targetPos);
            double disShooter = shooter.position().vectorTo(targetPos).horizontalDistance();
            double randomPos = Mth.cos((float) Mth.clamp(dis / disShooter, 0, 1) * 1.5f * Mth.PI) * dis * 10 * randomFloat;

            Vec3 toVec = this.position().vectorTo(targetPos).add(new Vec3(-randomPos, Mth.abs((float) randomPos) * 0.02, randomPos).scale(1 - Mth.clamp(0.02 * (tickCount - 20), 0, 1))).normalize();
            turn(toVec, 90);
            this.setDeltaMovement(this.getDeltaMovement().add(position().vectorTo(targetPos).normalize().scale(0.1)));

            if (dis2 < 1) {
                if (this.level() instanceof ServerLevel) {
                    causeMissileExplode(ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, this.getOwner()), this.explosionDamage, this.explosionRadius);
                }
                this.discard();
            }

            this.setDeltaMovement(this.getDeltaMovement().multiply(0.55, 0.55, 0.55));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.97, 0.97, 0.97));
        }

        if (this.tickCount > 13) {
            this.setDeltaMovement(this.getDeltaMovement().add(getLookAngle()));
        }

        if (this.tickCount > 300 || this.isInWater() || this.entityData.get(HEALTH) <= 0) {
            if (this.level() instanceof ServerLevel) {
                causeMissileExplode(ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, this.getOwner()), this.explosionDamage, this.explosionRadius);
            }
            this.discard();
        }
    }

    public void causeMissileExplode(@Nullable DamageSource source, float damage, float radius) {
        new CustomExplosion.Builder(this)
                .damageSource(source)
                .damage(damage)
                .radius(radius)
                .damageMultiplier(1.25F)
                .withParticleType(ParticleTool.ParticleType.MEDIUM)
                .explode();

        discard();
    }

    private PlayState movementPredicate(AnimationState<SwarmDroneEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.sd.fly"));
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
    public @NotNull SoundEvent getCloseSound() {
        return ModSounds.DRONE_ENGINE.get();
    }

    @Override
    public float getVolume() {
        return 0.6f;
    }

    @Override
    public float getMaxHealth() {
        return 4;
    }

    public void setRotate(Vec3 vec3) {
        double d0 = vec3.horizontalDistance();
        this.setYRot((float) (-Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) java.lang.Math.PI)));
        this.setXRot((float) (-Mth.atan2(vec3.y, d0) * (double) (180F / (float) java.lang.Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }


    @Override
    public void shoot(double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
        Vec3 vec3 = (new Vec3(pX, pY, pZ)).normalize().add(this.random.triangle(0.0D, 0.0172275D * (double) pInaccuracy), this.random.triangle(0.0D, 0.0172275D * (double) pInaccuracy), this.random.triangle(0.0D, 0.0172275D * (double) pInaccuracy)).scale((double) pVelocity);
        this.setDeltaMovement(vec3);
    }
}
