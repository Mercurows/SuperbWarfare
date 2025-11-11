package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimable;
import com.atsuishio.superbwarfare.entity.vehicle.base.CannonEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.entity.vehicle.weapon.SmallCannonShellWeapon;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.common.container.ContainerBlockItem;
import com.atsuishio.superbwarfare.tools.*;
import com.atsuishio.superbwarfare.world.TDMSavedData;
import com.mojang.math.Axis;
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
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
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
import java.util.function.Consumer;

public class Hpj11Entity extends VehicleEntity implements GeoEntity, CannonEntity, OwnableEntity, AutoAimable {

    public static Consumer<VehicleEntity> fireSound = vehicle -> {
    };

    public static final EntityDataAccessor<Integer> ANIM_TIME = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> GUN_ROTATE = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Boolean> ACTIVE = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.BOOLEAN);
    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.STRING);
    public static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.OPTIONAL_UUID);
    public static final EntityDataAccessor<Integer> FIRE_TIME = SynchedEntityData.defineId(Hpj11Entity.class, EntityDataSerializers.INT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private boolean wasFiring = false;

    public int changeTargetTimer = 60;

    public float gunRot;
    public float gunRotO;

    public Hpj11Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.HPJ_11.get(), world);
    }

    public Hpj11Entity(EntityType<Hpj11Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public int getContainerSize() {
        return 102;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANIM_TIME, 0);
        this.entityData.define(GUN_ROTATE, 0f);
        this.entityData.define(TARGET_UUID, "none");
        this.entityData.define(OWNER_UUID, Optional.empty());
        this.entityData.define(ACTIVE, false);
        this.entityData.define(FIRE_TIME, 0);
    }

    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(2 + 0.75 * ClientMouseHandler.custom3pDistanceLerp, 0.75, 0);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("AnimTime", this.entityData.get(ANIM_TIME));
        compound.putBoolean("Active", this.entityData.get(ACTIVE));
        if (this.getOwnerUUID() != null) {
            compound.putUUID("Owner", this.getOwnerUUID());
        }
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(ANIM_TIME, compound.getInt("AnimTime"));
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
                .custom((source, damage) -> getSourceAngle(source, 1f) * damage);
    }

    @Override
    public @NotNull Vec3 getDeltaMovement() {
        return new Vec3(0, Math.min(super.getDeltaMovement().y, 0), 0);
    }

    @Override
    public void baseTick() {
        if (!this.wasFiring && this.isFiring() && this.level().isClientSide()) {
            fireSound.accept(this);
        }
        this.wasFiring = this.isFiring();

        this.gunRotO = this.getGunRot();
        super.baseTick();

        if (this.entityData.get(ANIM_TIME) > 0) {
            this.entityData.set(ANIM_TIME, this.entityData.get(ANIM_TIME) - 1);
        }

        if (this.level() instanceof ServerLevel) {
            this.handleAmmo();
        }

        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.onGround()) {
            this.setDeltaMovement(Vec3.ZERO);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.04, 0));
        }

        if (this.getFirstPassenger() instanceof Player player && fireInputDown()) {
            if ((this.entityData.get(AMMO) > 0 || InventoryTool.hasCreativeAmmoBox(player)) && !cannotFire) {
                vehicleShoot(player);
            }
        }

        this.entityData.set(GUN_ROTATE, this.entityData.get(GUN_ROTATE) * 0.8f);
        setGunRot(getGunRot() + entityData.get(GUN_ROTATE));

        autoAim();

        if (entityData.get(FIRE_TIME) > 0) {
            entityData.set(FIRE_TIME, entityData.get(FIRE_TIME) - 1);
        }

        lowHealthWarning();
    }

    private void handleAmmo() {
        if (hasItem(ModItems.CREATIVE_AMMO_BOX.get())) {
            entityData.set(AMMO, 9999);
        } else {
            entityData.set(AMMO, countItem(ModItems.SMALL_SHELL.get()));
        }
    }

    public boolean isFiring() {
        return this.entityData.get(FIRE_TIME) > 0;
    }

    public void autoAim() {
        if (this.getFirstPassenger() != null || !entityData.get(ACTIVE)) {
            return;
        }

        if (this.getEnergy() <= VehicleConfig.HPJ11_SEEK_COST.get()) return;

        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0f, 0.4f, 0);
        Vec3 barrelRootPos = new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);

        if (entityData.get(TARGET_UUID).equals("none") && tickCount % 2 == 0) {
            Entity naerestEntity = seekNearLivingEntity(this, barrelRootPos, -32.5, 90, 3, 160, 0.3);
            if (naerestEntity != null) {
                entityData.set(TARGET_UUID, naerestEntity.getStringUUID());
                this.consumeEnergy(VehicleConfig.HPJ11_SEEK_COST.get());
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
                    vehicleShoot(player);
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

    public float getGunRot() {
        return this.gunRot;
    }

    public void setGunRot(float pGunRot) {
        this.gunRot = pGunRot;
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

    public Vec3 driverPos(float ticks) {
        Matrix4f transform = getVehicleFlatTransform(ticks);
        Vector4f worldPosition = transformPosition(transform, -1.0625f, 3f, -1.0625f);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public Vec3 getZoomPos(Entity entity, float partialTicks) {
        Matrix4f transform = getBarrelTransform(partialTicks);
        Vector4f worldPosition = transformPosition(transform, 0f, 0.5f, 0);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    @Override
    public void vehicleShoot(LivingEntity living) {
        if (cannotFire) return;
        if (this.getEnergy() < VehicleConfig.HPJ11_SHOOT_COST.get()) return;

        boolean hasCreativeAmmo = InventoryTool.hasCreativeAmmoBox(getFirstPassenger()) || hasItem(ModItems.CREATIVE_AMMO_BOX.get());

        entityData.set(FIRE_TIME, Math.min(entityData.get(FIRE_TIME) + 3, 5));

        var entityToSpawn = ((SmallCannonShellWeapon) getWeapon(0)).create(living);

        entityToSpawn.setPos(getShootPos(living, 1).x, getShootPos(living, 1).y, getShootPos(living, 1).z);
        entityToSpawn.shoot(getLookAngle().x, getLookAngle().y, getLookAngle().z, projectileVelocity(living), 0.25f);
        level().addFreshEntity(entityToSpawn);

        this.entityData.set(GUN_ROTATE, entityData.get(GUN_ROTATE) + 0.5f);
        this.entityData.set(ANIM_TIME, 1);

        this.consumeEnergy(VehicleConfig.HPJ11_SHOOT_COST.get());

        if (hasCreativeAmmo) return;

        this.getItemStacks().stream().filter(stack -> stack.is(ModItems.SMALL_SHELL.get())).findFirst().ifPresent(stack -> stack.shrink(1));
    }

    public float shootingVolume() {
        return entityData.get(FIRE_TIME) * 0.4f;
    }

    public float shootingPitch() {
        return 0.8f + entityData.get(FIRE_TIME) * 0.1f;
    }

    @Override
    public Vec3 getShootPos(int seatIndex, float ticks) {
        Matrix4f transform = getBarrelTransform(1);
        Vector4f worldPosition = transformPosition(transform, 0f, 0.35f, 0);
        return new Vec3(worldPosition.x, worldPosition.y, worldPosition.z);
    }

    // 炮弹发射速度
    @Override
    public float projectileVelocity(Entity entity) {
        return 20;
    }
    // 炮弹重力
    @Override
    public float projectileGravity(Entity entity) {
        return 0.03f;
    }

    public Matrix4f getBarrelTransform(float partialTicks) {
        Matrix4f transformV = getVehicleFlatTransform(partialTicks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, 1.375f, 0.25f);

        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformV.rotate(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, xRotO, getXRot())));
        return transformV;
    }

    @Override
    public void travel() {
        if (this.getEnergy() <= 0) return;
        Entity passenger = this.getFirstPassenger();
        if (passenger != null) {

            float diffY = Mth.wrapDegrees(passenger.getYHeadRot() - this.getYRot());
            float diffX = Mth.wrapDegrees(passenger.getXRot() - this.getXRot());

            turretTurnSound(diffX, diffY, 0.95f);

            this.setYRot(this.getYRot() + Mth.clamp(0.9f * diffY, -20f, 20f));
            this.setXRot(Mth.clamp(this.getXRot() + Mth.clamp(0.9f * diffX, -15f, 15f), -90, 32.5f));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public Vec3 getBarrelVector(float pPartialTicks) {
        return getViewVector(pPartialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public @Nullable Vec2 getCameraRotation(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            return new Vec2(Mth.lerp(partialTicks, player.yRotO, player.getYRot()), Mth.lerp(partialTicks, player.xRotO, player.getXRot()));
        }
        return super.getCameraRotation(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public Vec3 getCameraPosition(float partialTicks, Player player, boolean zoom, boolean isFirstPerson) {
        if (zoom || isFirstPerson) {
            if (zoom) {
                return new Vec3(this.getZoomPos(player, partialTicks).x, this.getZoomPos(player, partialTicks).y, this.getZoomPos(player, partialTicks).z);
            } else {
                return new Vec3(this.driverPos(partialTicks).x, this.driverPos(partialTicks).y, this.driverPos(partialTicks).z);
            }
        }
        return super.getCameraPosition(partialTicks, player, false, false);
    }

    @OnlyIn(Dist.CLIENT)
    public boolean useFixedCameraPos(Entity entity) {
        return true;
    }
}
