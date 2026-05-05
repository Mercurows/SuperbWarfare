package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.init.*;
import com.atsuishio.superbwarfare.item.projectile.MediumRocketItem;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.joml.Math;

import java.util.ArrayList;
import java.util.List;

public class Type63Entity extends GeoVehicleEntity {

    public static final EntityDataAccessor<Float> TARGET_PITCH = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> TARGET_YAW = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> BODY_YAW = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> SHOOT_PITCH = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> SHOOT_YAW = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<List<Integer>> LOADED_AMMO = SynchedEntityData.defineId(Type63Entity.class, ModSerializers.INT_LIST_SERIALIZER.get());

    public OBB[] barrel = new OBB[12];
    public OBB pitchController;
    public OBB yawController;
    public OBB hoe1;
    public OBB hoe2;
    public OBB wheel1;
    public OBB wheel2;
    public OBB body1;
    public OBB body2;

    public double interactionTick;
    public int cooldown;

    public Type63Entity(EntityType<Type63Entity> type, Level world) {
        super(type, world);
        this.wheel1 = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.125f, 0.390625f, 0.390625f), new Quaterniond(), OBB.Part.WHEEL_LEFT);
        this.wheel2 = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.125f, 0.390625f, 0.390625f), new Quaterniond(), OBB.Part.WHEEL_RIGHT);
        this.body1 = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.4765625f, 0.3515625f, 0.7578125f), new Quaterniond(), OBB.Part.BODY);
        this.body2 = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.771875f, 0.109375f, 0.296875f), new Quaterniond(), OBB.Part.BODY);

        this.barrel[0] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[1] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[2] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[3] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[4] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[5] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[6] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[7] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[8] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[9] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[10] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.barrel[11] = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.09375f, 0.09375f, 0.0625f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.pitchController = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.15625f, 0.21875f, 0.21875f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.yawController = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.125f, 0.125f, 0.125f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.hoe1 = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.125f, 0.125f, 0.875f), new Quaterniond(), OBB.Part.INTERACTIVE);
        this.hoe2 = new OBB(OBB.vec3ToVector3d(this.position()), new Vector3d(0.125f, 0.125f, 0.875f), new Quaterniond(), OBB.Part.INTERACTIVE);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public void playerTouch(Player pPlayer) {
        if (this.position().distanceTo(pPlayer.position()) > 1.4 || pPlayer == this.getFirstPassenger() || pPlayer.position().y > position().y || !pPlayer.isShiftKeyDown())
            return;
        if (!this.level().isClientSide
                && pPlayer.getY() < this.getY() + this.getBbHeight()
                && pPlayer.getY() + pPlayer.getBbHeight() > this.getY()
        ) {
            double entitySize = pPlayer.getBbWidth() * pPlayer.getBbHeight();
            double thisSize = this.getBbWidth() * this.getBbHeight();
            double f = Math.min(entitySize / thisSize, 2);
            this.setDeltaMovement(this.getDeltaMovement().add(new Vec3(pPlayer.position().vectorTo(this.position()).toVector3f()).scale(0.5 * f * pPlayer.getDeltaMovement().length())));
            this.setYRot(pPlayer.getYHeadRot());
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        var list = new ArrayList<Integer>();
        for (int i = 0; i < this.getContainerSize(); i++) {
            list.add(-1);
        }

        builder.define(TARGET_PITCH, 0F)
                .define(TARGET_YAW, 0F)
                .define(BODY_YAW, 0F)
                .define(SHOOT_PITCH, 0F)
                .define(SHOOT_YAW, 0F)
                .define(LOADED_AMMO, list);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Pitch", this.entityData.get(TARGET_PITCH));
        compound.putFloat("Yaw", this.entityData.get(TARGET_YAW));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(TARGET_PITCH, compound.getFloat("Pitch"));
        this.entityData.set(TARGET_YAW, compound.getFloat("Yaw"));
        setChanged();
    }

    @Override
    public @NotNull InteractionResult interact(@NotNull Player player, @NotNull InteractionHand hand) {
        var result = super.interact(player, hand);
        if (result != InteractionResult.PASS) return result;

        var stack = player.getMainHandItem();
        var lookingObb = OBB.getLookingObb(player, player.entityInteractionRange());

        if (stack.isEmpty()) {
            if (player.isShiftKeyDown()) {
                if (lookingObb == hoe1) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        entityData.set(BODY_YAW, entityData.get(BODY_YAW) + 0.2f * (float) interactionTick);
                        interactionTick++;
                        if (cooldown == 0) {
                            cooldown = 6;
                            Vec3 vec3 = OBB.vector3dToVec3(hoe1.center);
                            serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.WHEEL_VEHICLE_STEP.get(), SoundSource.PLAYERS, 0.5f, random.nextFloat() * 0.05f + 0.975f);
                        }
                    }
                    player.swing(InteractionHand.MAIN_HAND);
                }

                if (lookingObb == hoe2) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        entityData.set(BODY_YAW, entityData.get(BODY_YAW) - 0.2f * (float) interactionTick);
                        interactionTick++;
                        if (cooldown == 0) {
                            cooldown = 6;
                            Vec3 vec3 = OBB.vector3dToVec3(hoe1.center);
                            serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.WHEEL_VEHICLE_STEP.get(), SoundSource.PLAYERS, 0.5f, random.nextFloat() * 0.05f + 0.975f);
                        }
                    }
                    player.swing(InteractionHand.MAIN_HAND);
                }
            } else {
                // 取出炮弹
                player.swing(InteractionHand.MAIN_HAND);

                if (level() instanceof ServerLevel serverLevel && cooldown == 0) {
                    for (int i = 0; i < this.barrel.length; i++) {
                        if (lookingObb == this.barrel[i] && !getItems().get(i).isEmpty()) {
                            player.addItem(getItems().get(i).copyWithCount(1));
                            Vec3 vec3 = OBB.vector3dToVec3(this.barrel[i].center);
                            serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.TYPE_63_RELOAD.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.1f + 0.9f);
                            cooldown = 5;
                            getItems().set(i, ItemStack.EMPTY);
                            setChanged();
                        }
                    }
                }
            }

            if (lookingObb == yawController) {
                interactEvent(OBB.vector3dToVec3(yawController.center));
                entityData.set(TARGET_YAW, Mth.clamp(entityData.get(TARGET_YAW) + (player.isShiftKeyDown() ? -0.02f : 0.02f) * (float) interactionTick, -getTurretMaxYaw(), -getTurretMinYaw()));
                player.swing(InteractionHand.MAIN_HAND);
            }

            if (lookingObb == pitchController) {
                interactEvent(OBB.vector3dToVec3(pitchController.center));
                entityData.set(TARGET_PITCH, Mth.clamp(entityData.get(TARGET_PITCH) + (player.isShiftKeyDown() ? 0.02f : -0.02f) * (float) interactionTick, -getTurretMaxPitch(), -getTurretMinPitch()));
                player.swing(InteractionHand.MAIN_HAND);
            }

        }

        if (stack.getItem() instanceof MediumRocketItem) {
            for (int i = 0; i < this.barrel.length; i++) {
                if (lookingObb == this.barrel[i] && getItems().get(i).isEmpty() && level() instanceof ServerLevel serverLevel && cooldown == 0) {
                    this.setItem(i, stack.copyWithCount(1));
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    Vec3 vec3 = OBB.vector3dToVec3(this.barrel[i].center);
                    serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.TYPE_63_RELOAD.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.1f + 0.9f);
                    cooldown = 5;
                    setChanged();
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
        }

        if (cooldown == 0 && (stack.is(ModTags.Items.TOOLS_CROWBAR) || stack.is(Items.FLINT_AND_STEEL))) {
            // 发射
            if (lookingAtBarrel(player)) {
                // 精准发射
                for (int i = 0; i < this.barrel.length; i++) {
                    if (lookingObb == this.barrel[i] && getItems().get(i).getItem() instanceof MediumRocketItem) {
                        shoot(player, i);
                        getItems().set(i, ItemStack.EMPTY);
                        setChanged();
                    }
                }

                player.swing(InteractionHand.MAIN_HAND);
            } else {
                // 顺序发射
                for (int i = 0; i < 12; i++) {
                    if (getItems().get(i).getItem() instanceof MediumRocketItem) {
                        shoot(player, i);
                        getItems().set(i, ItemStack.EMPTY);
                        setChanged();

                        player.swing(InteractionHand.MAIN_HAND);
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }

        return InteractionResult.FAIL;
    }

    public boolean lookingAtBarrel(Player player) {
        var lookingObb = OBB.getLookingObb(player, player.entityInteractionRange());

        for (int i = 0; i < 12; i++) {
            if (lookingObb == barrel[i]) {
                return true;
            }
        }

        return false;
    }

    public void interactEvent(Vec3 vec3) {
        if (level() instanceof ServerLevel serverLevel) {
            interactionTick++;
            if (cooldown <= 0) {
                cooldown = 6;
                serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.HAND_WHEEL_ROT.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.05f + 0.975f);
            }
        }
    }

    public void shoot(Player player, int i) {
        ItemStack stack = getItems().get(i);

        if (!(stack.getItem() instanceof MediumRocketItem rocketItem)) {
            return;
        }

        var gunData = getGunData(rocketItem.type.toString());
        if (gunData == null) return;

        float shootVelocity = getProjectileVelocity(gunData);
        float shootSpread = getProjectileSpread(gunData);
        float shootGravity = getProjectileGravity(gunData);

        OBB obb = this.barrel[i];
        Vec3 shootPos = OBB.vector3dToVec3(obb.center);

        var entityToSpawn = new MediumRocketEntity(ModEntities.MEDIUM_ROCKET.get(), shootPos.x, shootPos.y, shootPos.z, level(),
                gunData.get(GunProp.DAMAGE).floatValue(), gunData.get(GunProp.EXPLOSION_RADIUS).floatValue(), gunData.get(GunProp.EXPLOSION_DAMAGE).floatValue(),
                0, 0, rocketItem.type, gunData.get(GunProp.SPREAD_AMOUNT), gunData.get(GunProp.SPREAD_ANGLE));
        entityToSpawn.durability(gunData.get(GunProp.AP_DURABILITY));
        entityToSpawn.setGravity(shootGravity);
        entityToSpawn.setOwner(player);

        var barrelVector = getBarrelVector(1);
        entityToSpawn.shoot(barrelVector.x, barrelVector.y, barrelVector.z, shootVelocity, shootSpread);
        level().addFreshEntity(entityToSpawn);

        level().playSound(null, shootPos.x, shootPos.y, shootPos.z, gunData.get(GunProp.SOUND_INFO).fire3P, SoundSource.PLAYERS, gunData.get(GunProp.SOUND_RADIUS).floatValue(), random.nextFloat() * 0.1f + 0.95f);

        AABB ab = new AABB(getBoundingBox().getCenter(), getBoundingBox().getCenter()).inflate(0.75).move(barrelVector.scale(-2)).expandTowards(barrelVector.scale(-5));

        // 尾焰
        for (var entity : level().getEntities(EntityTypeTest.forClass(Entity.class), ab, target -> target != this)) {
            entity.hurt(ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), player), 30 - 2 * entity.distanceTo(this));
            double force = 4 - 0.7 * entity.distanceTo(this);
            entity.push(-force * barrelVector.x, -force * barrelVector.y, -force * barrelVector.z);
        }

        cooldown = 10;
        if (level() instanceof ServerLevel serverLevel) {
            ParticleTool.spawnMediumCannonMuzzleParticles(barrelVector.scale(-1), shootPos.add(barrelVector.scale(-0.5)), serverLevel, this);
            ParticleTool.spawnMediumCannonMuzzleParticles(barrelVector.scale(-1), shootPos.add(barrelVector.scale(-1.5)), serverLevel, this);
            ParticleTool.spawnMediumCannonMuzzleParticles(barrelVector, shootPos.add(barrelVector.scale(1.5)), serverLevel, this);
        }

        gunData.shakePlayers(this);
    }

    @Override
    public void baseTick() {
        setTurretYRotO(this.getTurretYRot());
        setTurretXRotO(this.getTurretXRot());
        setLeftWheelRotO(this.getLeftWheelRot());
        setRightWheelRotO(this.getRightWheelRot());

        super.baseTick();

        double fluidFloat = 0.052 * VehicleVecUtils.getSubmergedHeight(this);
        this.setDeltaMovement(this.getDeltaMovement().add(0, fluidFloat, 0));

        if (this.onGround()) {
            float f0 = 0.35f + 0.5f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.05 * getDeltaMovement().dot(getViewVector(1)))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f0, 0.99, f0));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }

        if (this.isInWater()) {
            float f1 = (float) (0.7f - (0.04f * Math.min(VehicleVecUtils.getSubmergedHeight(this), this.getBbHeight())) + 0.08f * Mth.abs(90 - (float) VehicleVecUtils.calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90);
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.04 * getDeltaMovement().dot(getViewVector(1)))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f1, 0.85, f1));
        }

        if (cooldown > 0) {
            cooldown--;
        }

        interactionTick *= 0.94;

        if (level() instanceof ServerLevel) {
            entityData.set(SHOOT_PITCH, (float) VehicleVecUtils.getXRotFromVector(getBarrelVector(1)));
            entityData.set(SHOOT_YAW, (float) -VehicleVecUtils.getYRotFromVector(getBarrelVector(1)));
        }

        entityData.set(BODY_YAW, entityData.get(BODY_YAW) * 0.8f);
        setYRot(getYRot() + entityData.get(BODY_YAW));

        this.refreshDimensions();
    }

    @Override
    public void travel() {
        float diffY = entityData.get(TARGET_YAW) - getTurretYRot();
        this.setTurretYRot(Mth.clamp(this.getTurretYRot() + 0.1f * diffY, -getTurretMaxYaw(), -getTurretMinYaw()));

        float diffX = entityData.get(TARGET_PITCH) - getTurretXRot();
        this.setTurretXRot(Mth.clamp(this.getTurretXRot() + 0.1f * diffX, -getTurretMaxPitch(), -getTurretMinPitch()));

        double s0 = getDeltaMovement().dot(this.getViewVector(1));

        this.setLeftWheelRot((float) (this.getLeftWheelRot() - 1.167 * s0));
        this.setRightWheelRot((float) (this.getRightWheelRot() - 1.167 * s0));
    }

    public Vec3 getShootPos(float pPartialTicks) {
        Matrix4d transform = getBarrelTransform(pPartialTicks);
        Vector4d rootPosition = transformPosition(transform, 0, 0.000625, -0.44625);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return false;
    }

    @Override
    public @NotNull List<OBB> getOBBs() {
        return List.of(this.barrel[0], this.barrel[1], this.barrel[2], this.barrel[3], this.barrel[4], this.barrel[5], this.barrel[6], this.barrel[7], this.barrel[8], this.barrel[9], this.barrel[10], this.barrel[11],
                this.hoe1, this.hoe2, this.yawController, this.pitchController, this.wheel1, this.wheel2, this.body1, this.body2);
    }

    @Override
    public void updateOBB() {
        Matrix4d transform = getVehicleTransform(1);

        // 驻锄位置
        Vector4d worldPosition = transformPosition(transform, 0.875, 0.1875, -1.625);
        this.hoe1.center.set(new Vector3f((float) worldPosition.x, (float) worldPosition.y, (float) worldPosition.z));
        this.hoe1.updateRotation(VectorTool.combineRotations(1, this));

        Vector4d worldPosition2 = transformPosition(transform, -0.875, 0.1875, -1.625);
        this.hoe2.center.set(new Vector3f((float) worldPosition2.x, (float) worldPosition2.y, (float) worldPosition2.z));
        this.hoe2.updateRotation(VectorTool.combineRotations(1, this));

        Vector4d worldPositionW = transformPosition(transform, 0.90625, 0.390625, 0.1071875);
        this.wheel1.center.set(new Vector3f((float) worldPositionW.x, (float) worldPositionW.y, (float) worldPositionW.z));
        this.wheel1.updateRotation(VectorTool.combineRotations(1, this));

        Vector4d worldPositionW2 = transformPosition(transform, -0.90625, 0.390625, 0.1071875);
        this.wheel2.center.set(new Vector3f((float) worldPositionW2.x, (float) worldPositionW2.y, (float) worldPositionW2.z));
        this.wheel2.updateRotation(VectorTool.combineRotations(1, this));

        Vector4d worldPositionBody2 = transformPosition(transform, 0, 0.42546875, -0.090625);
        this.body2.center.set(new Vector3f((float) worldPositionBody2.x, (float) worldPositionBody2.y, (float) worldPositionBody2.z));
        this.body2.updateRotation(VectorTool.combineRotationsBarrel(1, this));

        Matrix4d transformT = getTurretTransform(1);

        Vector4d worldPositionYaw = transformPosition(transformT, 0.62625, 0.0396875, -0.5);
        this.yawController.center.set(new Vector3f((float) worldPositionYaw.x, (float) worldPositionYaw.y, (float) worldPositionYaw.z));
        this.yawController.updateRotation(VectorTool.combineRotationsTurret(1, this));

        Vector4d worldPositionPitch = transformPosition(transformT, 0.7825, 0.5771875, -0.024375);
        this.pitchController.center.set(new Vector3f((float) worldPositionPitch.x, (float) worldPositionPitch.y, (float) worldPositionPitch.z));
        this.pitchController.updateRotation(VectorTool.combineRotationsTurret(1, this));

        Matrix4d transformB = getBarrelTransform(1);

        double i = 0.24375f;

        setBarrelOBB(0, -0.3659375, 0.244375);
        setBarrelOBB(1, -0.3659375 + i, 0.244375);
        setBarrelOBB(2, -0.3659375 + 2 * i, 0.244375);
        setBarrelOBB(3, -0.3659375 + 3 * i, 0.244375);
        setBarrelOBB(4, -0.3659375, 0.244375 - i);
        setBarrelOBB(5, -0.3659375 + i, 0.244375 - i);
        setBarrelOBB(6, -0.3659375 + 2 * i, 0.244375 - i);
        setBarrelOBB(7, -0.3659375 + 3 * i, 0.244375 - i);
        setBarrelOBB(8, -0.3659375, 0.244375 - 2 * i);
        setBarrelOBB(9, -0.3659375 + i, 0.244375 - 2 * i);
        setBarrelOBB(10, -0.3659375 + 2 * i, 0.244375 - 2 * i);
        setBarrelOBB(11, -0.3659375 + 3 * i, 0.244375 - 2 * i);

        Vector4d worldPositionBody1 = transformPosition(transformB, 0, 0, 0.3740625);
        this.body1.center.set(new Vector3f((float) worldPositionBody1.x, (float) worldPositionBody1.y, (float) worldPositionBody1.z));
        this.body1.updateRotation(VectorTool.combineRotationsBarrel(1, this));
    }

    private void setBarrelOBB(int index, double x, double y) {
        Vector4d vec = transformPosition(getBarrelTransform(1), x, y, -0.44625);
        this.barrel[index].center.set(new Vector3f((float) vec.x, (float) vec.y, (float) vec.z));
        this.barrel[index].updateRotation(VectorTool.combineRotationsBarrel(1, this));
    }

    @Override
    public void setChanged() {
        super.setChanged();
        var list = new ArrayList<Integer>();
        for (var item : this.getItems()) {
            if (item.getItem() instanceof MediumRocketItem mediumRocketItem) {
                list.add(mediumRocketItem.type.ordinal());
            } else {
                list.add(-1);
            }
        }
        this.entityData.set(LOADED_AMMO, list);
    }
}
