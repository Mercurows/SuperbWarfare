package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimable;
import com.atsuishio.superbwarfare.entity.vehicle.base.EnergyVehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.item.ContainerBlockItem;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

import static com.atsuishio.superbwarfare.tools.ParticleTool.sendParticle;
import static com.atsuishio.superbwarfare.tools.SeekTool.smokeFilter;

public class LaserTowerEntity extends EnergyVehicleEntity implements GeoEntity, OwnableEntity, AutoAimable {

    public static final EntityDataAccessor<Integer> COOL_DOWN = SynchedEntityData.defineId(LaserTowerEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(LaserTowerEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(LaserTowerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(LaserTowerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Float> LASER_LENGTH = SynchedEntityData.defineId(LaserTowerEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public int changeTargetTimer = 60;

    public LaserTowerEntity(EntityType<LaserTowerEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public LaserTowerEntity(LivingEntity owner, Level level) {
        super(ModEntities.LASER_TOWER.get(), level);
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
        builder.define(TARGET_UUID, "none")
                .define(OWNER_UUID, Optional.empty())
                .define(COOL_DOWN, 0)
                .define(LASER_LENGTH, 0f)
                .define(ACTIVE, false);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("CoolDown", this.entityData.get(COOL_DOWN));
        compound.putBoolean("Active", this.entityData.get(ACTIVE));
        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(COOL_DOWN, compound.getInt("CoolDown"));
        this.entityData.set(ACTIVE, compound.getBoolean("Active"));

        UUID uuid;
        if (compound.hasUUID("Owner")) {
            uuid = compound.getUUID("Owner");
        } else {
            String s = compound.getString("Owner");

            assert this.getServer() != null;
            uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
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
            if (stack.is(ModItems.CROWBAR.get()) && (getOwner() == null || player == getOwner())) {
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
        super.baseTick();

        if (this.entityData.get(COOL_DOWN) > 0) {
            this.entityData.set(COOL_DOWN, this.entityData.get(COOL_DOWN) - 1);
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

    @Override
    public void destroy() {
        Entity attacker = EntityFindUtil.findEntity(this.level(), this.entityData.get(LAST_ATTACKER_UUID));
        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), attacker, attacker), 10f,
                    this.getX(), this.getY(), this.getZ(), 3f, Explosion.BlockInteraction.KEEP).setDamageMultiplier(1);
            explosion.explode();
            EventHooks.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
        }

        super.destroy();
    }

    public void autoAim() {
        if (this.getEnergy() <= 0 || !entityData.get(ACTIVE)) {
            return;
        }

        Vec3 barrelRootPos = new Vec3(this.getX(), this.getY() + 1.390625f, this.getZ());

        if (entityData.get(TARGET_UUID).equals("none") && tickCount % 10 == 0 && entityData.get(COOL_DOWN) == 0) {
            Entity naerestEntity = seekNearLivingEntity(this, barrelRootPos, -40, 90, 1, 72, 0.01);
            if (naerestEntity != null) {
                entityData.set(TARGET_UUID, naerestEntity.getStringUUID());
            }
        }

        Entity target = EntityFindUtil.findEntity(level(), entityData.get(TARGET_UUID));

        if (target != null && smokeFilter(target)) {
            if (target instanceof Player player1 && (player1.isSpectator() || player1.isCreative())) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (target.distanceTo(this) > 72) {
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
            if (target instanceof Projectile && (VectorTool.calculateAngle(target.getDeltaMovement().normalize(), target.position().vectorTo(this.position()).normalize()) > 60 || target.onGround())) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }

            Vec3 targetVec = barrelRootPos.vectorTo(target.getEyePosition()).normalize();

            double d0 = targetVec.x;
            double d1 = targetVec.y;
            double d2 = targetVec.z;
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            this.setXRot(Mth.clamp(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 57.2957763671875))), -90, 40));
            float targetY = Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90.0F);

            float diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(targetY - this.getYRot()));

            turretTurnSound(0, diffY, 1.1f);

            this.setYRot(this.getYRot() + Mth.clamp(0.5f * diffY, -60f, 60f));
            this.setRot(this.getYRot(), this.getXRot());

            if (this.entityData.get(COOL_DOWN) == 0 && VectorTool.calculateAngle(getViewVector(1), targetVec) < 1) {
                changeTargetTimer++;
            }

            if (this.entityData.get(COOL_DOWN) == 0 && VectorTool.calculateAngle(getViewVector(1), targetVec) < 1 && checkNoClip(this, target, barrelRootPos)) {
                this.entityData.set(COOL_DOWN, VehicleConfig.LASER_TOWER_COOLDOWN.get());

                if (level() instanceof ServerLevel serverLevel) {
                    this.level().playSound(this, getOnPos(), ModSounds.LASER_TOWER_SHOOT.get(), SoundSource.PLAYERS, 2, random.nextFloat() * 0.1f + 1);
                    sendParticle(serverLevel, ParticleTypes.END_ROD, target.getX(), target.getEyeY(), target.getZ(), 12, 0, 0, 0, 0.05, true);
                    sendParticle(serverLevel, ParticleTypes.LAVA, target.getX(), target.getEyeY(), target.getZ(), 4, 0, 0, 0, 0.15, true);
                }

                target.hurt(ModDamageTypes.causeLaserStaticDamage(this.level().registryAccess(), this, this.getOwner()), VehicleConfig.LASER_TOWER_DAMAGE.get());
                target.invulnerableTime = 0;
                entityData.set(LASER_LENGTH, distanceTo(target));
                if (Math.random() < 0.25 && target instanceof LivingEntity living) {
                    living.setRemainingFireTicks(2);
                }

                if (target instanceof Projectile) {
                    causeAirExplode(target.position());
                    target.discard();
                }

                if (!target.isAlive()) {
                    entityData.set(TARGET_UUID, "none");
                }
                this.consumeEnergy(VehicleConfig.LASER_TOWER_SHOOT_COST.get());
            }

        } else {
            entityData.set(TARGET_UUID, "none");
        }

        if (changeTargetTimer > 60) {
            entityData.set(TARGET_UUID, "none");
            changeTargetTimer = 0;
        }
    }

    @Override
    public boolean basicEnemyFilter(Entity pEntity) {
        if (pEntity instanceof Projectile) return false;
        if (this.getOwner() == null) return false;
        if (pEntity.getTeam() == null) return false;

        return !pEntity.isAlliedTo(this.getOwner()) || (pEntity.getTeam() != null && pEntity.getTeam().getName().equals("TDM"));
    }

    @Override
    public boolean basicEnemyProjectileFilter(Projectile projectile) {
        if (this.getOwner() == null) return false;
        if (projectile.getOwner() == null) return false;
        if (projectile.getOwner() == this.getOwner()) return false;
        return !projectile.getOwner().isAlliedTo(this.getOwner()) || (projectile.getOwner().getTeam() != null && projectile.getOwner().getTeam().getName().equals("TDM"));
    }

    private void causeAirExplode(Vec3 vec3) {
        CustomExplosion explosion = new CustomExplosion(this.level(), this,
                ModDamageTypes.causeProjectileBoomDamage(this.level().registryAccess(),
                        this,
                        this.getOwner()),
                5,
                vec3.x,
                vec3.y,
                vec3.z,
                1,
                Explosion.BlockInteraction.KEEP).
                setDamageMultiplier(1);
        explosion.explode();
        EventHooks.onExplosionStart(this.level(), explosion);
        explosion.finalizeExplosion(false);
        ParticleTool.spawnMediumExplosionParticles(this.level(), vec3);
    }

    private PlayState movementPredicate(AnimationState<LaserTowerEntity> event) {
        if (this.entityData.get(COOL_DOWN) > 10) {
            return event.setAndContinue(RawAnimation.begin().thenPlayAndHold("animation.lt.fire"));
        }
        return event.setAndContinue(RawAnimation.begin().thenLoop("animation.lt.idle"));
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
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/defense.png");
    }
}
