package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class Agm65Entity extends FastThrowableProjectile implements GeoEntity, ExplosiveProjectile {

    public static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(Agm65Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(Agm65Entity.class, EntityDataSerializers.STRING);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final DamageModifier DAMAGE_MODIFIER = DamageModifier.createDefaultModifier();
    private float damage = ExplosionConfig.AGM_65_DAMAGE.get();
    private float explosionDamage = ExplosionConfig.AGM_65_EXPLOSION_DAMAGE.get();
    private float explosionRadius = ExplosionConfig.AGM_65_EXPLOSION_RADIUS.get().floatValue();
    private boolean distracted = false;
    private int durability;
    public float gravity = 0.15f;

    public Agm65Entity(EntityType<? extends Agm65Entity> type, Level world) {
        super(type, world);
        this.noCulling = true;
        this.durability = 25;
    }

    public Agm65Entity(LivingEntity entity, Level level) {
        super(ModEntities.AGM_65.get(), entity, level);
        this.noCulling = true;
        this.durability = 25;
    }

    public void setTargetUuid(String uuid) {
        this.entityData.set(TARGET_UUID, uuid);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.AGM.get();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        amount = DAMAGE_MODIFIER.compute(source, amount);
        this.entityData.set(HEALTH, this.entityData.get(HEALTH) - amount);

        return super.hurt(source, amount);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);

        builder.define(HEALTH, 30f)
                .define(TARGET_UUID, "none");
    }

    @Override
    protected void updateRotation() {
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Health")) {
            this.entityData.set(HEALTH, compound.getFloat("Health"));
        }
        if (compound.contains("Damage")) {
            this.damage = compound.getFloat("Damage");
        }
        if (compound.contains("ExplosionDamage")) {
            this.explosionDamage = compound.getFloat("ExplosionDamage");
        }
        if (compound.contains("Radius")) {
            this.explosionRadius = compound.getFloat("Radius");
        }
        if (compound.contains("Durability")) {
            this.durability = compound.getInt("Durability");
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Health", this.entityData.get(HEALTH));
        compound.putFloat("Damage", this.damage);
        compound.putFloat("ExplosionDamage", this.explosionDamage);
        compound.putFloat("Radius", this.explosionRadius);
        compound.putInt("Durability", this.durability);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (entity == this.getOwner() || (this.getOwner() != null && entity == this.getOwner().getVehicle()))
            return;
        if (this.level() instanceof ServerLevel) {
            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                    PacketDistributor.sendToPlayer(player, new ClientIndicatorMessage(0, 5));
                }
            }

            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), this.damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            causeExplode(result.getLocation());

            discard();
        }
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
                    this.level().destroyBlock(resultPos, true);
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
    public void causeExplode(Vec3 vec3) {
        new CustomExplosion.Builder(this)
                .attacker(this.getOwner())
                .damage(explosionDamage)
                .radius(explosionRadius)
                .position(vec3)
                .withParticleType(ParticleTool.ParticleType.HUGE)
                .explode();
    }

    @Override
    public void tick() {
        super.tick();

        largeTrail();

        Entity entity = EntityFindUtil.findEntity(this.level(), entityData.get(TARGET_UUID));
        List<Entity> decoy = SeekTool.seekLivingEntities(this, 32, 90);

        for (var e : decoy) {
            if (e.getType().is(ModTags.EntityTypes.DECOY) && !this.distracted) {
                this.entityData.set(TARGET_UUID, e.getStringUUID());
                this.distracted = true;
                break;
            }
        }

        if (!entityData.get(TARGET_UUID).equals("none")) {
            if (entity != null) {
                if (entity.level() instanceof ServerLevel) {
                    if ((!entity.getPassengers().isEmpty() || entity instanceof VehicleEntity) && entity.tickCount % ((int) Math.max(0.04 * this.distanceTo(entity), 2)) == 0) {
                        entity.level().playSound(null, entity.getOnPos(), entity instanceof Pig ? SoundEvents.PIG_HURT : ModSounds.MISSILE_WARNING.get(), SoundSource.PLAYERS, 2, 1f);
                    }

                    Vec3 targetPos = new Vec3(entity.getX(), entity.getY() + (entity instanceof EnderDragon ? -2 : 0), entity.getZ());

                    Vec3 toVec = position().vectorTo(targetPos).normalize();
                    if (this.tickCount > 8) {
                        this.setDeltaMovement(this.getDeltaMovement().add(getLookAngle()));
                        boolean lostTarget = (VectorTool.calculateAngle(getLookAngle(), toVec) > 80);
                        if (!lostTarget) {
                            turn(toVec, 6);
                        }
                    }
                }
            }
        }

        if (this.tickCount == 8) {
            this.level().playSound(null, BlockPos.containing(position()), ModSounds.MISSILE_START.get(), SoundSource.PLAYERS, 4, 1);
            if (!this.level().isClientSide() && this.level() instanceof ServerLevel serverLevel) {
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CLOUD, this.xo, this.yo, this.zo, 15, 0.8, 0.8, 0.8, 0.01, true);
                ParticleTool.sendParticle(serverLevel, ParticleTypes.CAMPFIRE_COSY_SMOKE, this.xo, this.yo, this.zo, 10, 0.8, 0.8, 0.8, 0.01, true);
            }
        }

        if (this.tickCount > 600 || this.isInWater() || this.entityData.get(HEALTH) <= 0) {
            if (this.level() instanceof ServerLevel) {
                ProjectileTool.causeCustomExplode(this,
                        ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), this, this.getOwner()),
                        this, this.explosionDamage, this.explosionRadius, 1);
            }
            this.discard();
        }

        this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 0.8, 0.8));

        destroyBlock();
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    private PlayState movementPredicate(AnimationState<Agm65Entity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.jvm.idle"));
    }

    @Override
    protected double getDefaultGravity() {
        return tickCount > 8 ? 0 : this.gravity;
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

    @Override
    public @NotNull SoundEvent getCloseSound() {
        return ModSounds.ROCKET_ENGINE.get();
    }

    @Override
    public @NotNull SoundEvent getSound() {
        return ModSounds.ROCKET_FLY.get();
    }

    @Override
    public float getVolume() {
        return 0.7f;
    }

    @Override
    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void setExplosionDamage(float explosionDamage) {
        this.explosionDamage = explosionDamage;
    }

    @Override
    public void setExplosionRadius(float radius) {
        this.explosionRadius = radius;
    }

    @Override
    public boolean forceLoadChunk() {
        return true;
    }

    @Override
    public void setGravity(float gravity) {
        this.gravity = gravity;
    }

    @Override
    public void shoot(double pX, double pY, double pZ, float pVelocity, float pInaccuracy) {
        Vec3 vec3 = (new Vec3(pX, pY, pZ)).normalize().add(this.random.triangle(0.0D, 0.0172275D * (double)pInaccuracy), this.random.triangle(0.0D, 0.0172275D * (double)pInaccuracy), this.random.triangle(0.0D, 0.0172275D * (double)pInaccuracy)).scale((double)pVelocity);
        this.setDeltaMovement(vec3);
        double d0 = vec3.horizontalDistance();
        this.setYRot((float)(-Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float) java.lang.Math.PI)));
        this.setXRot((float)(-Mth.atan2(vec3.y, d0) * (double)(180F / (float) java.lang.Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }
}
