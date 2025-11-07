package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.projectile.MediumRocketEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSerializers;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.common.ammo.MediumRocketItem;
import com.atsuishio.superbwarfare.network.message.receive.ShakeClientMessage;
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
import net.minecraft.world.Container;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class Type63Entity extends VehicleEntity implements GeoEntity, OBBEntity {

    public static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> BODY_YAW = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> SHOOT_PITCH = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> SHOOT_YAW = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<List<Integer>> LOADED_AMMO = SynchedEntityData.defineId(Type63Entity.class, ModSerializers.INT_LIST_SERIALIZER.get());

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

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
        this.wheel1 = new OBB(this.position().toVector3f(), new Vector3f(0.125f, 0.390625f, 0.390625f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.wheel2 = new OBB(this.position().toVector3f(), new Vector3f(0.125f, 0.390625f, 0.390625f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.body1 = new OBB(this.position().toVector3f(), new Vector3f(0.4765625f, 0.3515625f, 0.7578125f), new Quaternionf(), OBB.Part.BODY);
        this.body2 = new OBB(this.position().toVector3f(), new Vector3f(0.771875f, 0.109375f, 0.296875f), new Quaternionf(), OBB.Part.BODY);

        this.barrel[0] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[1] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[2] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[3] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[4] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[5] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[6] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[7] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[8] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[9] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[10] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.barrel[11] = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.pitchController = new OBB(this.position().toVector3f(), new Vector3f(0.15625f, 0.21875f, 0.21875f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.yawController = new OBB(this.position().toVector3f(), new Vector3f(0.125f, 0.125f, 0.125f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.hoe1 = new OBB(this.position().toVector3f(), new Vector3f(0.125f, 0.125f, 0.875f), new Quaternionf(), OBB.Part.INTERACTIVE);
        this.hoe2 = new OBB(this.position().toVector3f(), new Vector3f(0.125f, 0.125f, 0.875f), new Quaternionf(), OBB.Part.INTERACTIVE);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        var list = new ArrayList<Integer>();
        for (int i = 0; i < this.getContainerSize(); i++) {
            list.add(-1);
        }

        builder.define(PITCH, 0F)
                .define(YAW, 0F)
                .define(BODY_YAW, 0F)
                .define(SHOOT_PITCH, 0F)
                .define(SHOOT_YAW, 0F)
                .define(LOADED_AMMO, list);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Pitch", this.entityData.get(PITCH));
        compound.putFloat("Yaw", this.entityData.get(YAW));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(PITCH, compound.getFloat("Pitch"));
        this.entityData.set(YAW, compound.getFloat("Yaw"));
        setChanged();
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 1;
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        var result = super.interact(player, hand);
        if (result != InteractionResult.PASS) return result;

        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            if (player.isShiftKeyDown()) {
                if (OBB.getLookingObb(player, player.entityInteractionRange()) == hoe1) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        entityData.set(BODY_YAW, entityData.get(BODY_YAW) + 0.2f * (float) interactionTick);
                        interactionTick++;
                        if (cooldown == 0) {
                            cooldown = 6;
                            Vec3 vec3 = new Vec3(hoe1.center());
                            serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.WHEEL_STEP.get(), SoundSource.PLAYERS, 0.5f, random.nextFloat() * 0.05f + 0.975f);
                        }
                    }
                    player.swing(InteractionHand.MAIN_HAND);
                }
                if (OBB.getLookingObb(player, player.entityInteractionRange()) == hoe2) {
                    if (player.level() instanceof ServerLevel serverLevel) {
                        entityData.set(BODY_YAW, entityData.get(BODY_YAW) - 0.2f * (float) interactionTick);
                        interactionTick++;
                        if (cooldown == 0) {
                            cooldown = 6;
                            Vec3 vec3 = new Vec3(hoe1.center());
                            serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.WHEEL_STEP.get(), SoundSource.PLAYERS, 0.5f, random.nextFloat() * 0.05f + 0.975f);
                        }
                    }
                    player.swing(InteractionHand.MAIN_HAND);
                }
            }

            if (OBB.getLookingObb(player, player.entityInteractionRange()) == yawController) {
                interactEvent(new Vec3(yawController.center()));
                entityData.set(YAW, Mth.clamp(entityData.get(YAW) + (player.isShiftKeyDown() ? -0.02f : 0.02f) * (float) interactionTick, -turretMaxYaw(), -turretMinYaw()));
                player.swing(InteractionHand.MAIN_HAND);
            }
            if (OBB.getLookingObb(player, player.entityInteractionRange()) == pitchController) {
                interactEvent(new Vec3(pitchController.center()));
                entityData.set(PITCH, Mth.clamp(entityData.get(PITCH) + (player.isShiftKeyDown() ? 0.02f : -0.02f) * (float) interactionTick, -turretMaxPitch(), -turretMinPitch()));
                player.swing(InteractionHand.MAIN_HAND);
            }

            // 取出炮弹

            if (player.isShiftKeyDown()) {
                for (int i = 0; i < this.barrel.length; i++) {
                    if (OBB.getLookingObb(player, player.entityInteractionRange()) == this.barrel[i] && !items.get(i).isEmpty() && level() instanceof ServerLevel serverLevel && cooldown == 0) {
                        player.addItem(items.get(i).copyWithCount(1));
                        Vec3 vec3 = new Vec3(this.barrel[i].center());
                        serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.TYPE_63_RELOAD.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.1f + 0.9f);
                        cooldown = 5;
                        items.set(i, ItemStack.EMPTY);
                        setChanged();
                    }
                    player.swing(InteractionHand.MAIN_HAND);
                }
            }

        }

        if (stack.getItem() instanceof MediumRocketItem) {
            for (int i = 0; i < this.barrel.length; i++) {
                if (OBB.getLookingObb(player, player.entityInteractionRange()) == this.barrel[i] && items.get(i).isEmpty() && level() instanceof ServerLevel serverLevel && cooldown == 0) {
                    this.setItem(i, stack.copyWithCount(1));
                    if (!player.isCreative()) {
                        stack.shrink(1);
                    }
                    Vec3 vec3 = new Vec3(this.barrel[i].center());
                    serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.TYPE_63_RELOAD.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.1f + 0.9f);
                    cooldown = 5;
                    setChanged();
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
        }

        if (stack.is(ModTags.Items.TOOLS_CROWBAR) || stack.is(Items.FLINT_AND_STEEL)) {
            // 发射
            if (lookingAtBarrel(player)) {
                // 精准发射
                for (int i = 0; i < this.barrel.length; i++) {
                    if (OBB.getLookingObb(player, player.entityInteractionRange()) == this.barrel[i] && items.get(i).getItem() instanceof MediumRocketItem && cooldown == 0) {
                        shoot(player, i);
                        items.set(i, ItemStack.EMPTY);
                        setChanged();
                        player.swing(InteractionHand.MAIN_HAND);
                    }
                    player.swing(InteractionHand.MAIN_HAND);
                }
            } else {
                // 顺序发射
                for (int i = 0; i < 12; i++) {
                    if (items.get(i).getItem() instanceof MediumRocketItem && cooldown == 0) {
                        shoot(player, i);
                        items.set(i, ItemStack.EMPTY);
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
        for (int i = 0; i < 12; i++) {
            if (OBB.getLookingObb(player, player.entityInteractionRange()) == barrel[i]) {
                return true;
            }
        }
        return false;
    }

    public void interactEvent(Vec3 vec3) {
        if (level() instanceof ServerLevel serverLevel) {
            interactionTick++;
            interactionTick++;
            if (cooldown == 0) {
                cooldown = 6;
                serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.HAND_WHEEL_ROT.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.05f + 0.975f);
            }
        }
    }

    public void shoot(Player player, int i) {
        ItemStack stack = items.get(i);

        if (!(stack.getItem() instanceof MediumRocketItem rocketItem)) {
            return;
        }

        OBB obb = this.barrel[i];
        Vec3 shootPos = new Vec3(obb.center());

        MediumRocketEntity entityToSpawn = rocketItem.createProjectile(level(), shootPos);
        entityToSpawn.setOwner(player);
        entityToSpawn.shoot(getShootVector(1).x, getShootVector(1).y, getShootVector(1).z, 10, (float) 0.75);
        level().addFreshEntity(entityToSpawn);
        level().playSound(null, shootPos.x, shootPos.y, shootPos.z, ModSounds.MEDIUM_ROCKET_FIRE.get(), SoundSource.PLAYERS, 4f, random.nextFloat() * 0.1f + 0.95f);

        AABB ab = new AABB(getBoundingBox().getCenter(), getBoundingBox().getCenter()).inflate(0.75).move(getShootVector(1).scale(-2)).expandTowards(getShootVector(1).scale(-5));

        for (var entity : level().getEntities(EntityTypeTest.forClass(Entity.class), ab,
                target -> target != this && target != getFirstPassenger() && target.getVehicle() == null)
        ) {
            entity.hurt(ModDamageTypes.causeBurnDamage(entity.level().registryAccess(), player), 30 - 2 * entity.distanceTo(this));
            double force = 4 - 0.7 * entity.distanceTo(this);
            entity.push(-force * getShootVector(1).x, -force * getShootVector(1).y, -force * getShootVector(1).z);
        }

        cooldown = 10;
        if (level() instanceof ServerLevel serverLevel) {
            ParticleTool.spawnMediumCannonMuzzleParticles(getShootVector(1).scale(-1), shootPos.add(getShootVector(1).scale(-0.5)), serverLevel, this);
            ParticleTool.spawnMediumCannonMuzzleParticles(getShootVector(1).scale(-1), shootPos.add(getShootVector(1).scale(-1.5)), serverLevel, this);
            ParticleTool.spawnMediumCannonMuzzleParticles(getShootVector(1), shootPos.add(getShootVector(1).scale(1.5)), serverLevel, this);
            for (int j = 0; j < 20; j += 4) {
                Mod.queueServerWork(j, () -> ParticleTool.spawnBarrelSmoke(1, serverLevel, getShootVector(1), shootPos.add(getShootVector(1).scale(1.3))));
            }
        }

        ShakeClientMessage.sendToNearbyPlayers(this, 8, 8, 10, 20);
    }

    @Override
    public void baseTick() {
        turretYRotO = this.getTurretYRot();
        turretXRotO = this.getTurretXRot();
        leftWheelRotO = this.getLeftWheelRot();
        rightWheelRotO = this.getRightWheelRot();

        super.baseTick();
        updateOBB();

        double fluidFloat = 0.052 * VehicleVecUtils.getSubmergedHeight(this);
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, fluidFloat, 0.0));

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

        interactionTick *= 0.96;

        if (level() instanceof ServerLevel) {
            entityData.set(SHOOT_PITCH, (float) VehicleVecUtils.getXRotFromVector(getShootVector(1)));
            entityData.set(SHOOT_YAW, (float) -VehicleVecUtils.getYRotFromVector(getShootVector(1)));
        }

        entityData.set(BODY_YAW, entityData.get(BODY_YAW) * 0.8f);
        setYRot(getYRot() + entityData.get(BODY_YAW));

        this.refreshDimensions();
    }

    @Override
    public void travel() {
        float diffY = entityData.get(YAW) - getTurretYRot();
        this.setTurretYRot(Mth.clamp(this.getTurretYRot() + 0.1f * diffY, -15, 15));

        float diffX = entityData.get(PITCH) - getTurretXRot();
        this.setTurretXRot(Mth.clamp(this.getTurretXRot() + 0.1f * diffX, -60, 5));

        double s0 = getDeltaMovement().dot(this.getViewVector(1));

        this.setLeftWheelRot((float) (this.getLeftWheelRot() - 1.167 * s0));
        this.setRightWheelRot((float) (this.getRightWheelRot() - 1.167 * s0));
    }

    public Vec3 getShootVector(float pPartialTicks) {
        Matrix4f transform = getBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    public Vec3 getShootPos(float pPartialTicks) {
        Matrix4f transform = getBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0.000625f, -0.44625f);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
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
    public boolean canTakeItem(@NotNull Container target, int slot, @NotNull ItemStack stack) {
        return false;
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.barrel[0], this.barrel[1], this.barrel[2], this.barrel[3], this.barrel[4], this.barrel[5], this.barrel[6], this.barrel[7], this.barrel[8], this.barrel[9], this.barrel[10], this.barrel[11],
                this.hoe1, this.hoe2, this.yawController, this.pitchController, this.wheel1, this.wheel2, this.body1, this.body2);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        // 驻锄位置
        Vector4f worldPosition = transformPosition(transform, 0.875f, 0.1875f, -1.625f);
        this.hoe1.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.hoe1.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, -0.875f, 0.1875f, -1.625f);
        this.hoe2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.hoe2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPositionW = transformPosition(transform, 0.90625f, 0.390625f, 0.1071875f);
        this.wheel1.center().set(new Vector3f(worldPositionW.x, worldPositionW.y, worldPositionW.z));
        this.wheel1.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPositionW2 = transformPosition(transform, -0.90625f, 0.390625f, 0.1071875f);
        this.wheel2.center().set(new Vector3f(worldPositionW2.x, worldPositionW2.y, worldPositionW2.z));
        this.wheel2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPositionBody2 = transformPosition(transform, 0, 0.42546875f, -0.090625f);
        this.body2.center().set(new Vector3f(worldPositionBody2.x, worldPositionBody2.y, worldPositionBody2.z));
        this.body2.setRotation(VectorTool.combineRotationsBarrel(1, this));

        Matrix4f transformT = getTurretTransform(1);

        Vector4f worldPositionYaw = transformPosition(transformT, 0.62625f, 0.0396875f, -0.5f);
        this.yawController.center().set(new Vector3f(worldPositionYaw.x, worldPositionYaw.y, worldPositionYaw.z));
        this.yawController.setRotation(VectorTool.combineRotationsTurret(1, this));

        Vector4f worldPositionPitch = transformPosition(transformT, 0.7825f, 0.5771875f, -0.024375f);
        this.pitchController.center().set(new Vector3f(worldPositionPitch.x, worldPositionPitch.y, worldPositionPitch.z));
        this.pitchController.setRotation(VectorTool.combineRotationsTurret(1, this));

        Matrix4f transformB = getBarrelTransform(1);

        float i = 0.24375f;

        setBarrelOBB(0, -0.3659375f, 0.244375f, -0.44625f);
        setBarrelOBB(1, -0.3659375f + i, 0.244375f, -0.44625f);
        setBarrelOBB(2, -0.3659375f + 2 * i, 0.244375f, -0.44625f);
        setBarrelOBB(3, -0.3659375f + 3 * i, 0.244375f, -0.44625f);
        setBarrelOBB(4, -0.3659375f, 0.244375f - i, -0.44625f);
        setBarrelOBB(5, -0.3659375f + i, 0.244375f - i, -0.44625f);
        setBarrelOBB(6, -0.3659375f + 2 * i, 0.244375f - i, -0.44625f);
        setBarrelOBB(7, -0.3659375f + 3 * i, 0.244375f - i, -0.44625f);
        setBarrelOBB(8, -0.3659375f, 0.244375f - 2 * i, -0.44625f);
        setBarrelOBB(9, -0.3659375f + i, 0.244375f - 2 * i, -0.44625f);
        setBarrelOBB(10, -0.3659375f + 2 * i, 0.244375f - 2 * i, -0.44625f);
        setBarrelOBB(11, -0.3659375f + 3 * i, 0.244375f - 2 * i, -0.44625f);

        Vector4f worldPositionBody1 = transformPosition(transformB, 0, 0, 0.3740625f);
        this.body1.center().set(new Vector3f(worldPositionBody1.x, worldPositionBody1.y, worldPositionBody1.z));
        this.body1.setRotation(VectorTool.combineRotationsBarrel(1, this));
    }

    private void setBarrelOBB(int index, float x, float y, float z) {
        Vector4f vec = transformPosition(getBarrelTransform(1), x, y, z);
        this.barrel[index].center().set(new Vector3f(vec.x, vec.y, vec.z));
        this.barrel[index].setRotation(VectorTool.combineRotationsBarrel(1, this));
    }

    @Override
    public void setChanged() {
        var list = new ArrayList<Integer>();
        for (var item : this.items) {
            if (item.getItem() instanceof MediumRocketItem mediumRocketItem) {
                list.add(mediumRocketItem.type.ordinal());
            } else {
                list.add(-1);
            }
        }
        this.entityData.set(LOADED_AMMO, list);
    }

    @Override
    public boolean hasEnergyStorage() {
        return false;
    }
}
