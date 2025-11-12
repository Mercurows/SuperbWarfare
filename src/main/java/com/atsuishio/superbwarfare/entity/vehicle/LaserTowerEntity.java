package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.OldUsersConverter;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

public class LaserTowerEntity extends VehicleEntity implements GeoEntity, OwnableEntity {

    public static final EntityDataAccessor<Integer> COOL_DOWN = SynchedEntityData.defineId(LaserTowerEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(LaserTowerEntity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(LaserTowerEntity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(LaserTowerEntity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Float> LASER_LENGTH = SynchedEntityData.defineId(LaserTowerEntity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public int changeTargetTimer = 60;

    public LaserTowerEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.LASER_TOWER.get(), world);
    }

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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_UUID, "none");
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(COOL_DOWN, 0);
        this.entityData.define(LASER_LENGTH, 0f);
        this.entityData.define(ACTIVE, false);
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
        super.baseTick();

        if (this.entityData.get(COOL_DOWN) > 0) {
            this.entityData.set(COOL_DOWN, this.entityData.get(COOL_DOWN) - 1);
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
        }
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
    public void lerpTo(double x, double y, double z, float yaw, float pitch, int interpolationSteps, boolean interpolate) {
        serverYRot = yaw;
        serverXRot = pitch;
        this.interpolationSteps = 10;
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
}
