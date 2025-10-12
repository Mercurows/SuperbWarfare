package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.network.message.receive.ClientIndicatorMessage;
import com.atsuishio.superbwarfare.tools.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
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
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class JavelinMissileEntity extends FastThrowableProjectile implements GeoEntity, ExplosiveProjectile {

    public static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(JavelinMissileEntity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(JavelinMissileEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> TOP = SynchedEntityData.defineId(JavelinMissileEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private float gravity = 0f;
    private float damage = 500.0f;
    private float explosionDamage = 140f;
    private float explosionRadius = 6f;
    private boolean distracted = false;
    private int guideType = 0;

    public float targetX;

    public float targetY;

    public float targetZ;

    public JavelinMissileEntity(EntityType<? extends JavelinMissileEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public JavelinMissileEntity(LivingEntity entity, Level level, float damage, float explosionDamage, float explosionRadius, int guideType, Vec3 targetPos) {
        super(ModEntities.JAVELIN_MISSILE.get(), entity, level);
        this.noCulling = true;

        this.damage = damage;
        this.explosionDamage = explosionDamage;
        this.explosionRadius = explosionRadius;
        this.guideType = guideType;
        this.targetX = (float) targetPos.x;
        this.targetY = (float) targetPos.y;
        this.targetZ = (float) targetPos.z;
        this.durability = 50;
    }

    public JavelinMissileEntity(PlayMessages.SpawnEntity spawnEntity, Level level) {
        this(ModEntities.JAVELIN_MISSILE.get(), level);
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ModItems.JAVELIN_MISSILE.get();
    }

    public void setTargetUuid(String uuid) {
        this.entityData.set(TARGET_UUID, uuid);
    }

    public void setAttackMode(boolean mode) {
        this.entityData.set(TOP, mode);
    }

    private static final DamageModifier DAMAGE_MODIFIER = DamageModifier.createDefaultModifier();

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        amount = DAMAGE_MODIFIER.compute(source, amount);
        this.entityData.set(HEALTH, this.entityData.get(HEALTH) - amount);

        return super.hurt(source, amount);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HEALTH, 10f);
        this.entityData.define(TARGET_UUID, "none");
        this.entityData.define(TOP, false);
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
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Health", this.entityData.get(HEALTH));
        compound.putFloat("Damage", this.damage);
        compound.putFloat("ExplosionDamage", this.explosionDamage);
        compound.putFloat("Radius", this.explosionRadius);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        Entity entity = result.getEntity();
        if (this.getOwner() != null && this.getOwner().getVehicle() != null && entity == this.getOwner().getVehicle())
            return;
        if (this.level() instanceof ServerLevel) {
            if (entity == this.getOwner() || (this.getOwner() != null && entity == this.getOwner().getVehicle()))
                return;
            if (this.getOwner() instanceof LivingEntity living) {
                if (!living.level().isClientSide() && living instanceof ServerPlayer player) {
                    living.level().playSound(null, living.blockPosition(), ModSounds.INDICATION.get(), SoundSource.VOICE, 1, 1);

                    Mod.PACKET_HANDLER.send(PacketDistributor.PLAYER.with(() -> player), new ClientIndicatorMessage(0, 5));
                }
            }

            DamageHandler.doDamage(entity, ModDamageTypes.causeProjectileHitDamage(this.level().registryAccess(), this, this.getOwner()), (entityData.get(TOP) ? 1.25f : 1f) * this.damage);

            if (entity instanceof LivingEntity) {
                entity.invulnerableTime = 0;
            }

            causeExplode(result.getLocation());
            this.discard();
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
    protected void updateRotation() {
    }

    @Override
    public void tick() {
        super.tick();

        mediumTrail();

        Entity entity = EntityFindUtil.findEntity(this.level(), entityData.get(TARGET_UUID));
        List<Entity> decoy = SeekTool.seekLivingEntities(this, 32, 90);

        for (var e : decoy) {
            if (e.getType().is(ModTags.EntityTypes.DECOY) && !this.distracted) {
                this.entityData.set(TARGET_UUID, e.getStringUUID());
                this.distracted = true;
                break;
            }
        }

        if (guideType == 0 || !entityData.get(TARGET_UUID).equals("none")) {
            if (entity != null) {
                boolean dir = position().vectorTo(entity.position()).horizontalDistanceSqr() < 900;
                Vec3 targetPos = new Vec3(entity.getX(), entity.getY() + 0.5f * entity.getBbHeight() + (entity instanceof EnderDragon ? -3 : 0), entity.getZ());
                Vec3 toVec = position().vectorTo(targetPos).normalize();
                if ((!entity.getPassengers().isEmpty() || entity instanceof VehicleEntity) && entity.tickCount % ((int) Math.max(0.04 * this.distanceTo(entity), 2)) == 0) {
                    entity.level().playSound(null, entity.getOnPos(), entity instanceof Pig ? SoundEvents.PIG_HURT : ModSounds.MISSILE_WARNING.get(), SoundSource.PLAYERS, 2, 1f);
                }
                if (this.tickCount > 3) {
                    this.setDeltaMovement(this.getDeltaMovement().add(getLookAngle()));
                    if (entityData.get(TOP)) {
                        if (!dir) {
                            Vec3 targetTopPos = new Vec3(targetPos.x, targetPos.y + Mth.clamp(6 * this.tickCount, 0, 90),targetPos.z);
                            Vec3 toTopVec = position().vectorTo(targetTopPos).normalize();
                            turn(toTopVec, 6);
                        } else {
                            boolean lostTarget = this.getY() < entity.getY();
                            if (!lostTarget) {
                                turn(toVec, 45);
                                this.setDeltaMovement(this.getDeltaMovement().scale(0.1).add(getLookAngle().scale(8)));
                            }
                        }
                    } else {
                        boolean lostTarget = (VectorTool.calculateAngle(getLookAngle(), toVec) > 80);
                        if (!lostTarget) {
                            turn(toVec, 6);
                        }
                    }
                }
            }
        } else if (guideType == 1) {
            Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
            boolean dir = position().vectorTo(targetPos).horizontalDistanceSqr() < 900;
            Vec3 toVec = getEyePosition().vectorTo(targetPos).normalize();

            if (this.tickCount > 3) {
                if (entityData.get(TOP)) {
                    if (!dir) {
                        Vec3 targetTopPos = new Vec3(targetPos.x, targetPos.y + Mth.clamp(5 * this.tickCount, 0, 90), targetPos.z);
                        Vec3 toTopVec = getEyePosition().vectorTo(targetTopPos).normalize();
                        turn(toTopVec, 6);
                    } else {
                        boolean lostTarget = this.getY() < targetPos.y;
                        if (!lostTarget) {
                            turn(toVec, 45);
                            this.setDeltaMovement(this.getDeltaMovement().add(getLookAngle().scale(2)));
                        }
                    }
                } else {
                    boolean lostTarget = (VectorTool.calculateAngle(getDeltaMovement(), toVec) > 80);
                    if (!lostTarget) {
                        turn(toVec, 6);
                    }
                }
            }

        }

        if (this.tickCount > 200 || this.isInWater() || this.entityData.get(HEALTH) <= 0) {
            if (this.level() instanceof ServerLevel) {
                ProjectileTool.causeCustomExplode(this,
                        ModDamageTypes.causeProjectileExplosionDamage(this.level().registryAccess(), this, this.getOwner()),
                        this, this.explosionDamage, this.explosionRadius);
            }
            this.discard();
        }

        this.setDeltaMovement(this.getDeltaMovement().multiply(0.76, 0.76, 0.76));
        destroyBlock();
    }

    private PlayState movementPredicate(AnimationState<JavelinMissileEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.jvm.idle"));
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
        return 0.4f;
    }

    @Override
    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    public void setExplosionDamage(float damage) {
        this.explosionDamage = damage;
    }

    @Override
    public void setExplosionRadius(float radius) {
        this.explosionRadius = radius;
    }

    @Override
    public float getGravity() {
        return this.gravity;
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
        this.setYRot((float)(-Mth.atan2(vec3.x, vec3.z) * (double)(180F / (float)Math.PI)));
        this.setXRot((float)(-Mth.atan2(vec3.y, d0) * (double)(180F / (float)Math.PI)));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }
}
