package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimable;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.WeaponVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.RangeTool;
import com.atsuishio.superbwarfare.tools.SeekTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.atsuishio.superbwarfare.world.TDMSavedData;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
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
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

public class Hpj11Entity extends VehicleEntity implements GeoEntity, OwnableEntity, AutoAimable, WeaponVehicleEntity {
    public static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.OPTIONAL_UUID);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public int changeTargetTimer = 60;

    public Hpj11Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.HPJ_11.get(), world);
    }

    public Hpj11Entity(EntityType<Hpj11Entity> type, Level world) {
        super(type, world);
    }



    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TARGET_UUID, "none");
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(ACTIVE, false);
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(2 + 0.75 * ClientMouseHandler.custom3pDistanceLerp, 0.75, 0);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Active", this.entityData.get(ACTIVE));
        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
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
        entityData.set(TARGET_UUID, "none");
        return super.interact(player, hand);
    }


    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.5f) * damage);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        lowHealthWarning();
    }

    public void autoAim() {
        if (this.getFirstPassenger() != null || !entityData.get(ACTIVE)) {
            return;
        }

//        if (this.getEnergy() <= VehicleConfig.HPJ11_SEEK_COST.get()) return;

        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0f, 0.4f, 0);
        Vec3 barrelRootPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        if (entityData.get(TARGET_UUID).equals("none") && tickCount % 2 == 0) {
            Entity naerestEntity = seekNearLivingEntity(this, barrelRootPos, -32.5, 90, 3, 160, 0.3);
            if (naerestEntity != null) {
                entityData.set(TARGET_UUID, naerestEntity.getStringUUID());
//                this.consumeEnergy(VehicleConfig.HPJ11_SEEK_COST.get());
            }
        }

        Entity target = EntityFindUtil.findEntity(level(), entityData.get(TARGET_UUID));

        if (target != null && this.getOwner() instanceof Player player && SeekTool.NOT_IN_SMOKE.test(target)) {
            if (target instanceof Player player1 && (player1.isSpectator() || player1.isCreative())) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }
            if (VehicleVecUtils.getSubmergedHeight(target) >= target.getBbHeight()) {
                this.entityData.set(TARGET_UUID, "none");
                return;
            }

            if (target.distanceTo(this) > 160) {
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

            if (target.getVehicle() != null) {
                this.entityData.set(TARGET_UUID, target.getVehicle().getStringUUID());
            }

            Vec3 targetPos = target.getBoundingBox().getCenter();
            Vec3 targetVel = target.getDeltaMovement();

            Vec3 targetVec = RangeTool.calculateFiringSolution(barrelRootPos, targetPos, targetVel, projectileVelocity(player), projectileGravity(player));

            double d0 = targetVec.x;
            double d1 = targetVec.y;
            double d2 = targetVec.z;
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);

            float targetY = Mth.wrapDegrees((float) (Mth.atan2(d2, d0) * 57.2957763671875) - 90F);
            float diffY = Math.clamp(-90f, 90f, Mth.wrapDegrees(targetY - this.getYRot()));

            turretTurnSound(0, diffY, 1.1f);

            this.setXRot(Mth.clamp(Mth.wrapDegrees((float) (-(Mth.atan2(d1, d3) * 57.2957763671875))), -90, 40));
            this.setYRot(this.getYRot() + Mth.clamp(0.9f * diffY, -20f, 20f));

            if (target.distanceTo(this) <= 144 && VectorTool.calculateAngle(getViewVector(1), targetVec) < 10) {
                if (checkNoClip(this, target, barrelRootPos) && entityData.get(AMMO) > 0) {
//                    vehicleShoot(player);
                } else {
                    changeTargetTimer++;
                }

                if (!target.isAlive()) {
                    entityData.set(TARGET_UUID, "none");
                }
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
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }
}
