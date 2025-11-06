package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.projectile.DestroyableProjectile;
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimable;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleHelper;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.tools.*;
import com.atsuishio.superbwarfare.world.TDMSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;
import static com.atsuishio.superbwarfare.tools.SeekTool.IS_FRIENDLY;

public class WaveforceTowerEntity extends VehicleEntity implements GeoEntity, OwnableEntity, AutoAimable {

    public static final EntityDataAccessor<Integer> COOL_DOWN = SynchedEntityData.defineId(WaveforceTowerEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Integer> CHARGED_ENERGY = SynchedEntityData.defineId(WaveforceTowerEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(WaveforceTowerEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(WaveforceTowerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(WaveforceTowerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Float> WAVEFORCE_LENGTH = SynchedEntityData.defineId(WaveforceTowerEntity.class, EntityDataSerializers.FLOAT);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public int changeTargetTimer = 60;
    public int chargeTime = 60;
    public int maxChargeEnergy = 250000;
    public float damage = 350;

    public WaveforceTowerEntity(EntityType<WaveforceTowerEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public WaveforceTowerEntity(LivingEntity owner, Level level) {
        super(ModEntities.WAVEFORCE_TOWER.get(), level);
        if (owner != null) {
            this.setOwnerUUID(owner.getUUID());
        }
    }

    public boolean isOwnedBy(LivingEntity pEntity) {
        return pEntity == this.getOwner();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);

        builder.define(COOL_DOWN, 0)
                .define(TARGET_UUID, "none")
                .define(OWNER_UUID, Optional.empty())
                .define(CHARGED_ENERGY, 0)
                .define(WAVEFORCE_LENGTH, 0f)
                .define(ACTIVE, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("ChargedEnergy", this.entityData.get(CHARGED_ENERGY));
        compound.putBoolean("Active", this.entityData.get(ACTIVE));
        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(CHARGED_ENERGY, compound.getInt("ChargedEnergy"));
        this.entityData.set(ACTIVE, compound.getBoolean("Active"));

        UUID uuid;
        if (compound.hasUUID("Owner")) {
            uuid = compound.getUUID("Owner");
        } else {
            String s = compound.getString("Owner");

            try {
                if (this.getServer() == null) {
                    uuid = UUID.fromString(s);
                } else {
                    uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
                }
            } catch (Exception exception) {
                Mod.LOGGER.error("Couldn't load owner UUID of {}: {}", this, exception);
                uuid = null;
            }
        }

        if (uuid != null) {
            try {
                this.setOwnerUUID(uuid);
            } catch (Throwable ignored) {
            }
        }
    }

    public void setOwnerUUID(@Nullable UUID pUuid) {
        this.entityData.set(OWNER_UUID, Optional.ofNullable(pUuid));
    }

    @Nullable
    public UUID getOwnerUUID() {
        return this.entityData.get(OWNER_UUID).orElse(null);
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (player.isCrouching()) {
            if (stack.is(ModTags.Items.TOOLS_CROWBAR) && (getOwner() == null || player == getOwner())) {
                ItemStack container = ContainerBlockItem.createInstance(this);
                if (!player.addItem(container)) {
                    player.drop(container, false);
                }
                this.remove(RemovalReason.DISCARDED);
                this.discard();
                return InteractionResult.SUCCESS;
            } else {
                if (this.getOwnerUUID() == null) {
                    this.setOwnerUUID(player.getUUID());
                }
                if (this.getOwner() == player) {
                    entityData.set(ACTIVE, !entityData.get(ACTIVE));

                    if (player instanceof ServerPlayer serverPlayer) {
                        serverPlayer.level().playSound(null, serverPlayer.getOnPos(), SoundEvents.ARROW_HIT_PLAYER, SoundSource.PLAYERS, 0.5F, 1);
                    }
                    return InteractionResult.sidedSuccess(this.level().isClientSide());
                } else {
                    return InteractionResult.PASS;
                }
            }
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return new Vec3(0, Math.min(super.getDeltaMovement().y, 0), 0);
    }

    @Override
    public void baseTick() {
        turretYRotO = this.getTurretYRot();
        turretXRotO = this.getTurretXRot();
        super.baseTick();

        if (this.entityData.get(COOL_DOWN) > 0) {
            this.entityData.set(COOL_DOWN, this.entityData.get(COOL_DOWN) - 1);
        }

        if (this.entityData.get(CHARGED_ENERGY) < maxChargeEnergy) {
            float chargeSpeed = (float) Mth.clamp(maxChargeEnergy / chargeTime, 0, getEnergy());
            this.entityData.set(CHARGED_ENERGY, (int) Mth.clamp(this.entityData.get(CHARGED_ENERGY) + chargeSpeed, 0, maxChargeEnergy));
            this.consumeEnergy((int) Mth.clamp(chargeSpeed, 0, maxChargeEnergy - this.entityData.get(CHARGED_ENERGY)));
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.04, 0.0));
        }
        this.autoAim();
    }

    @Override
    public void handleClientSync() {
        if (isControlledByLocalInstance()) {
            interpolationSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (interpolationSteps <= 0) {
            return;
        }

        double interpolatedYaw = Mth.wrapDegrees(serverYRot - (double) getYRot());
        setYRot(getYRot() + (float) interpolatedYaw / (float) interpolationSteps);
        setXRot(getXRot() + (float) (serverXRot - (double) getXRot()) / (float) interpolationSteps);
        setRot(getYRot(), getXRot());
    }

    @Override
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps) {
        serverYRot = yaw;
        serverXRot = pitch;
        this.interpolationSteps = 10;
    }

    public void autoAim() {
        if (!entityData.get(ACTIVE)) {
            return;
        }

        if (entityData.get(TARGET_UUID).equals("none") && tickCount % 10 == 0) {
            Entity naerestEntity = seekNearLivingEntity(this, getShootPos(1), -50, 50, 2, 128, 0.01);
            if (naerestEntity != null) {
                entityData.set(TARGET_UUID, naerestEntity.getStringUUID());
            }
        }

        Entity target = EntityFindUtil.findEntity(level(), entityData.get(TARGET_UUID));

        if (target != null && SeekTool.NOT_IN_SMOKE.test(target)) {
            if (target instanceof Player player1 && (player1.isSpectator() || player1.isCreative())) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (VehicleHelper.getSubmergedHeight(target) >= target.getBbHeight()) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (target.distanceTo(this) > 144) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (target instanceof LivingEntity living && living.getHealth() <= 0) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (target == this || target instanceof TargetEntity) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (target instanceof Projectile && (VectorTool.calculateAngle(target.getDeltaMovement().normalize(), target.position().vectorTo(this.position()).normalize()) > 60 || target.onGround() || target.getDeltaMovement().lengthSqr() < 0.001)) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }

            Vec3 targetVec = getShootPos(1).vectorTo(target.getEyePosition()).normalize();
            if (this.entityData.get(COOL_DOWN) == 0) {
                turretAutoAimFormVector(targetVec);
            }

            boolean canShoot = this.entityData.get(CHARGED_ENERGY) >= maxChargeEnergy;

            if (canShoot && VectorTool.calculateAngle(getBarrelVector(1), targetVec) < 1) {
                changeTargetTimer++;
            }

            if (canShoot && VectorTool.calculateAngle(getBarrelVector(1), targetVec) < 1 && checkNoClip(this, target, getShootPos(1))) {
                if (level() instanceof ServerLevel serverLevel) {
                    SoundTool.playDistantSound(serverLevel, ModSounds.WAVEFORCE_TOWER_FIRE.get(), position(), 6, random.nextFloat() * 0.1f + 1, null);
                }

                Predicate<Entity> filter = entity -> entity != this && !IS_FRIENDLY.test(this.getOwner(), entity);
                List<TraceTool.RayTraceResultEntity> hitList = TraceTool.getEntitiesAlongVector(level(), getShootPos(1), getBarrelVector(1), getShootPos(1).distanceTo(target.getEyePosition()) + 0.5, filter);
                for (TraceTool.RayTraceResultEntity hit : hitList) {
                    Entity entity = hit.entity;
                    Vec3 hitPos = hit.hitVec;
                    if (level() instanceof ServerLevel serverLevel) {
                        sendParticle(serverLevel, ParticleTypes.END_ROD, hitPos.x, hitPos.y, hitPos.z, 12, 0, 0, 0, 0.05, true);
                        sendParticle(serverLevel, ParticleTypes.LAVA, hitPos.x, hitPos.y, hitPos.z, 4, 0, 0, 0, 0.15, true);
                    }
                    DamageHandler.doDamage(entity, ModDamageTypes.causeLaserStaticDamage(this.level().registryAccess(), this, this.getOwner()), damage * Math.min((float) entityData.get(CHARGED_ENERGY) / maxChargeEnergy, 1));
                    target.invulnerableTime = 0;
                    if (Math.random() < 0.5 && target instanceof LivingEntity living) {
                        living.setRemainingFireTicks(5 * 20);
                    }

                    if (target instanceof Projectile && !(target instanceof DestroyableProjectile)) {
                        causeAirExplode(target.position());
                        target.discard();
                    }

                }

                entityData.set(WAVEFORCE_LENGTH, (float) getLaserPos(1).distanceTo(target.getEyePosition()));

                if (!target.isAlive()) {
                    entityData.set(TARGET_UUID, "none");
                }
                this.entityData.set(CHARGED_ENERGY, 0);
                this.entityData.set(COOL_DOWN, 25);
            }

        } else {
            entityData.set(TARGET_UUID, "none");
        }

        if (changeTargetTimer > 60) {
            entityData.set(TARGET_UUID, "none");
            changeTargetTimer = 0;
        }
    }

    private void causeAirExplode(Vec3 vec3) {
        createCustomExplosion()
                .damage(5)
                .radius(1)
                .keepBlock()
                .attacker(getOwner())
                .position(vec3)
                .withParticleType(ParticleTool.ParticleType.MEDIUM)
                .explode();
    }

    @Override
    public float turretYSpeed() {
        return 8;
    }

    @Override
    public float turretXSpeed() {
        return 12;
    }

    @Override
    public float turretMinPitch() {
        return -45;
    }

    @Override
    public float turretMaxPitch() {
        return 40;
    }

    @Override
    public boolean basicEnemyFilter(Entity pEntity) {
        if (pEntity instanceof Projectile) return false;
        if (this.getOwner() == null) return false;
        if (pEntity.getTeam() == null) return false;

        return !pEntity.isAlliedTo(this.getOwner()) || (pEntity.getTeam() != null && TDMSavedData.enabledTDM(pEntity));
    }

    @Override
    public boolean basicEnemyProjectileFilter(Projectile projectile) {
        if (this.getOwner() == null) return false;
        if (projectile.getOwner() != null && projectile.getOwner() == this.getOwner()) return false;
        return (projectile.getOwner() != null && !projectile.getOwner().isAlliedTo(this.getOwner()))
                || (projectile.getOwner() != null && projectile.getOwner().getTeam() != null && TDMSavedData.enabledTDM(projectile.getOwner()))
                || projectile.getOwner() == null;
    }

    @Override
    public Vec3 getTurretPosition() {
        return new Vec3(0, 2, -0.05843125);
    }

    @Override
    public Vec3 getBarrelPosition() {
        return new Vec3(0, 0.41615625, -0.02555);
    }

    public Vec3 getShootPos(float pPartialTicks) {
        Matrix4f transform = getBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0.5243625f, 0);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z);
    }

    public Vec3 getLaserPos(float pPartialTicks) {
        Matrix4f transform = getBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0.5243625f, 3.02875625f);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z);
    }

    private PlayState firePredicate(AnimationState<WaveforceTowerEntity> event) {
        if (this.entityData.get(COOL_DOWN) > 0) {
            return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.waveforce_tower.fire"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.waveforce_tower.idle"));
    }

    private PlayState barrelLightPredicate(AnimationState<WaveforceTowerEntity> event) {
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.waveforce_tower.idle"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "barrelLight", 0, this::barrelLightPredicate));
        data.add(new AnimationController<>(this, "fire", 0, this::firePredicate));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
