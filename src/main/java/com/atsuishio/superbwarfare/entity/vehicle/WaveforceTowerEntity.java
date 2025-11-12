package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
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
import org.joml.Matrix4f;
import org.joml.Vector4f;
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

public class WaveforceTowerEntity extends VehicleEntity implements GeoEntity, OwnableEntity{

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

    public WaveforceTowerEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.WAVEFORCE_TOWER.get(), world);
    }

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
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COOL_DOWN, 0);
        this.entityData.define(TARGET_UUID, "none");
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(CHARGED_ENERGY, 0);
        this.entityData.define(WAVEFORCE_LENGTH, 0f);
        this.entityData.define(ACTIVE, false);
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
