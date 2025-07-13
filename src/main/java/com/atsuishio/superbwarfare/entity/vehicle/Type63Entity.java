package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.item.SmallShellItem;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.mojang.math.Axis;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.wrapper.InvWrapper;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Math;
import org.joml.*;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class Type63Entity extends MobileVehicleEntity implements GeoEntity, OBBEntity, Container {

    public static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    public OBB barrel0;
    public OBB barrel1;
    public OBB barrel2;
    public OBB barrel3;
    public OBB barrel4;
    public OBB barrel5;
    public OBB barrel6;
    public OBB barrel7;
    public OBB barrel8;
    public OBB barrel9;
    public OBB barrel10;
    public OBB barrel11;
    public OBB pitchController;
    public OBB yawController;
    public OBB hoe1;
    public OBB hoe2;

    public double interactionTick;

    private LazyOptional<?> itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    public ItemStack stack = ItemStack.EMPTY;

    public Type63Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.TYPE_63.get(), world);
    }

    public Type63Entity(EntityType<Type63Entity> type, Level world) {
        super(type, world);
        this.barrel0 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel1 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel2 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel3 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel4 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel5 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel6 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel7 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel8 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel9 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel10 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.barrel11 = new OBB(this.position().toVector3f(), new Vector3f(0.09375f, 0.09375f, 0.0625f), new Quaternionf(), OBB.Part.BODY);
        this.pitchController = new OBB(this.position().toVector3f(), new Vector3f(0.15625f, 0.21875f, 0.21875f), new Quaternionf(), OBB.Part.BODY);
        this.yawController = new OBB(this.position().toVector3f(), new Vector3f(0.125f, 0.125f, 0.125f), new Quaternionf(), OBB.Part.BODY);
        this.hoe1 = new OBB(this.position().toVector3f(), new Vector3f(0.125f, 0.125f, 0.875f), new Quaternionf(), OBB.Part.BODY);
        this.hoe2 = new OBB(this.position().toVector3f(), new Vector3f(0.125f, 0.125f, 0.875f), new Quaternionf(), OBB.Part.BODY);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PITCH, 0f);
        this.entityData.define(YAW, 0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Pitch", this.entityData.get(PITCH));
        compound.putFloat("Yaw", this.entityData.get(YAW));
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(PITCH, compound.getFloat("Pitch"));
        this.entityData.set(YAW, compound.getFloat("Yaw"));
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            if (OBB.getLookingObb(player, player.getEntityReach()) == hoe1) {
                if (player.level() instanceof ServerLevel) {
                    setYRot(getYRot() + (float) interactionTick);
                    interactionTick++;
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
            if (OBB.getLookingObb(player, player.getEntityReach()) == hoe2) {
                if (player.level() instanceof ServerLevel) {
                    setYRot(getYRot() - (float) interactionTick);
                    interactionTick++;
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
            if (OBB.getLookingObb(player, player.getEntityReach()) == yawController) {
                if (player.level() instanceof ServerLevel) {
                    interactionTick++;
                }
                entityData.set(YAW, Mth.clamp(entityData.get(YAW) + (player.isShiftKeyDown() ? -0.01f : 0.01f) * (float) interactionTick, -15, 15));
                player.swing(InteractionHand.MAIN_HAND);
            }
            if (OBB.getLookingObb(player, player.getEntityReach()) == pitchController) {
                if (player.level() instanceof ServerLevel) {
                    interactionTick++;
                }
                entityData.set(PITCH, Mth.clamp(entityData.get(PITCH) + (player.isShiftKeyDown() ? 0.02f : -0.02f) * (float) interactionTick, -60, 5));
                player.swing(InteractionHand.MAIN_HAND);
            }
        }

        return InteractionResult.FAIL;
    }

    @Override
    public void baseTick() {
        turretYRotO = this.getTurretYRot();
        turretXRotO = this.getTurretXRot();
        leftWheelRotO = this.getLeftWheelRot();
        rightWheelRotO = this.getRightWheelRot();

        super.baseTick();
        updateOBB();

        double fluidFloat = 0.052 * getSubmergedHeight(this);
        this.setDeltaMovement(this.getDeltaMovement().add(0.0, fluidFloat, 0.0));

        if (this.onGround()) {
            float f0 = 0.35f + 0.5f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90;
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.05 * getDeltaMovement().dot(getViewVector(1)))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f0, 0.99, f0));
        } else {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.99, 0.99, 0.99));
        }

        if (this.isInWater()) {
            float f1 = (float) (0.7f - (0.04f * Math.min(getSubmergedHeight(this), this.getBbHeight())) + 0.08f * Mth.abs(90 - (float) calculateAngle(this.getDeltaMovement(), this.getViewVector(1))) / 90);
            this.setDeltaMovement(this.getDeltaMovement().add(this.getViewVector(1).normalize().scale(0.04 * getDeltaMovement().dot(getViewVector(1)))));
            this.setDeltaMovement(this.getDeltaMovement().multiply(f1, 0.85, f1));
        }

//        setTurretYRot(getTurretYRot() + 1);
//        setTurretXRot(getTurretXRot() + 1);

        interactionTick *= 0.96;

        this.refreshDimensions();
    }

    @Override
    public void destroy() {
        if (level() instanceof ServerLevel) {
            CustomExplosion explosion = new CustomExplosion(this.level(), this,
                    ModDamageTypes.causeCustomExplosionDamage(this.level().registryAccess(), getAttacker(), getAttacker()), 20f,
                    this.getX(), this.getY(), this.getZ(), 2f, ExplosionConfig.EXPLOSION_DESTROY.get() ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.KEEP, true).setDamageMultiplier(1);
            explosion.explode();
            net.minecraftforge.event.ForgeEventFactory.onExplosionStart(this.level(), explosion);
            explosion.finalizeExplosion(false);
            ParticleTool.spawnMediumExplosionParticles(this.level(), this.position());
        }

        explodePassengers();
        super.destroy();
    }

    @Override
    public void travel() {
        float diffY = entityData.get(YAW) - getTurretYRot();
        this.setTurretYRot(Mth.clamp(this.getTurretYRot() + 0.1f * diffY, -15, 15));

        float diffX = entityData.get(PITCH) - getTurretXRot();
        this.setTurretXRot(Mth.clamp(this.getTurretXRot() + 0.1f * diffX, -60, 5));

        double s0 = getDeltaMovement().dot(this.getViewVector(1));

        this.setLeftWheelRot((float) (this.getLeftWheelRot() - 1.75 * s0));
        this.setRightWheelRot((float) (this.getRightWheelRot() - 1.75 * s0));
    }

    @Override
    public Matrix4f getTurretTransform(float ticks) {
        Matrix4f transformV = getVehicleTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, 0.45703125f, -0.1625f);

        transformV.translate(worldPosition.x, worldPosition.y, worldPosition.z);
        transformV.rotate(Axis.YP.rotationDegrees(Mth.lerp(ticks, turretYRotO, getTurretYRot())));
        return transformV;
    }

    public Matrix4f getBarrelTransform(float ticks) {
        Matrix4f transformT = getTurretTransform(ticks);

        Matrix4f transform = new Matrix4f();
        Vector4f worldPosition = transformPosition(transform, 0, 0.65f, -0.203125f);

        transformT.translate(worldPosition.x, worldPosition.y, worldPosition.z);

        float x = Mth.lerp(ticks, turretXRotO, getTurretXRot());
        transformT.rotate(Axis.XP.rotationDegrees(x));
        return transformT;
    }

    public Vec3 getShootVector(float pPartialTicks) {
        Matrix4f transform = getBarrelTransform(pPartialTicks);
        Vector4f rootPosition = transformPosition(transform, 0, 0, 0);
        Vector4f targetPosition = transformPosition(transform, 0, 0, 1);
        return new Vec3(rootPosition.x, rootPosition.y, rootPosition.z).vectorTo(new Vec3(targetPosition.x, targetPosition.y, targetPosition.z));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }


    @Override
    public ResourceLocation getVehicleIcon() {
        return Mod.loc("textures/vehicle_icon/lav150_icon.png");
    }

    @Override
    public @Nullable ResourceLocation getVehicleItemIcon() {
        return Mod.loc("textures/gui/vehicle/type/defense.png");
    }

    @Override
    public int getContainerSize() {
        return 12;
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return stack == ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack getItem(int slot) {
        return slot == 0 ? stack : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || amount <= 0 || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        stack.shrink(1);
        if (stack.isEmpty()) {
            stack = ItemStack.EMPTY;
        }
        return stack;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int slot) {
        return removeItem(0, 1);
    }

    @Override
    public void setItem(int slot, @NotNull ItemStack stack) {
        if (slot != 0) return;
        this.stack = stack;
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return false;
    }

    @Override
    public void clearContent() {
        this.stack = ItemStack.EMPTY;
    }

    @Override
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        if (slot != 0) return false;
        return stack.getItem() instanceof SmallShellItem;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction facing) {
        if (this.isAlive() && capability == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandler.cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return this.getCapability(cap, null);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandler.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        itemHandler = LazyOptional.of(() -> new InvWrapper(this));
    }

    @Override
    public List<OBB> getOBBs() {
        return List.of(this.barrel0, this.barrel1, this.barrel2, this.barrel3, this.barrel4, this.barrel5, this.barrel6, this.barrel7, this.barrel8, this.barrel9, this.barrel10, this.barrel11,
                this.hoe1, this.hoe2, this.yawController, this.pitchController);
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

        Matrix4f transformT = getTurretTransform(1);

        Vector4f worldPositionYaw = transformPosition(transformT, 0.62625f, 0.0396875f, -0.5f);
        this.yawController.center().set(new Vector3f(worldPositionYaw.x, worldPositionYaw.y, worldPositionYaw.z));
        this.yawController.setRotation(VectorTool.combineRotationsTurret(1, this));

        Vector4f worldPositionPitch = transformPosition(transformT, 0.7825f, 0.5771875f, -0.024375f);
        this.pitchController.center().set(new Vector3f(worldPositionPitch.x, worldPositionPitch.y, worldPositionPitch.z));
        this.pitchController.setRotation(VectorTool.combineRotationsTurret(1, this));

        Matrix4f transformB = getBarrelTransform(1);

        float i = 0.24375f;

        Vector4f worldPositionBarrel0 = transformPosition(transformB, -0.3659375f, 0.244375f, -0.44625f);
        this.barrel0.center().set(new Vector3f(worldPositionBarrel0.x, worldPositionBarrel0.y, worldPositionBarrel0.z));
        this.barrel0.setRotation(VectorTool.combineRotationsBarrel(1, this));

        Vector4f worldPositionBarrel1 = transformPosition(transformB, -0.3659375f + i, 0.244375f, -0.44625f);
        this.barrel1.center().set(new Vector3f(worldPositionBarrel1.x, worldPositionBarrel1.y, worldPositionBarrel1.z));
        this.barrel1.setRotation(VectorTool.combineRotationsBarrel(1, this));

        Vector4f worldPositionBarrel2 = transformPosition(transformB, -0.3659375f + 2 * i, 0.244375f, -0.44625f);
        this.barrel2.center().set(new Vector3f(worldPositionBarrel2.x, worldPositionBarrel2.y, worldPositionBarrel2.z));
        this.barrel2.setRotation(VectorTool.combineRotationsBarrel(1, this));

        Vector4f worldPositionBarrel3 = transformPosition(transformB, -0.3659375f + 3 * i, 0.244375f, -0.44625f);
        this.barrel3.center().set(new Vector3f(worldPositionBarrel3.x, worldPositionBarrel3.y, worldPositionBarrel3.z));
        this.barrel3.setRotation(VectorTool.combineRotationsBarrel(1, this));


        Vector4f worldPositionBarrel4 = transformPosition(transformB, -0.3659375f, 0.244375f - i, -0.44625f);
        this.barrel4.center().set(new Vector3f(worldPositionBarrel4.x, worldPositionBarrel4.y, worldPositionBarrel4.z));
        this.barrel4.setRotation(VectorTool.combineRotationsBarrel(1, this));

        Vector4f worldPositionBarrel5 = transformPosition(transformB, -0.3659375f + i, 0.244375f - i, -0.44625f);
        this.barrel5.center().set(new Vector3f(worldPositionBarrel5.x, worldPositionBarrel5.y, worldPositionBarrel5.z));
        this.barrel5.setRotation(VectorTool.combineRotationsBarrel(1, this));

        Vector4f worldPositionBarrel6 = transformPosition(transformB, -0.3659375f + 2 * i, 0.244375f - i, -0.44625f);
        this.barrel6.center().set(new Vector3f(worldPositionBarrel6.x, worldPositionBarrel6.y, worldPositionBarrel6.z));
        this.barrel6.setRotation(VectorTool.combineRotationsBarrel(1, this));

        Vector4f worldPositionBarrel7 = transformPosition(transformB, -0.3659375f + 3 * i, 0.244375f - i, -0.44625f);
        this.barrel7.center().set(new Vector3f(worldPositionBarrel7.x, worldPositionBarrel7.y, worldPositionBarrel7.z));
        this.barrel7.setRotation(VectorTool.combineRotationsBarrel(1, this));


        Vector4f worldPositionBarrel8 = transformPosition(transformB, -0.3659375f, 0.244375f - 2 * i, -0.44625f);
        this.barrel8.center().set(new Vector3f(worldPositionBarrel8.x, worldPositionBarrel8.y, worldPositionBarrel8.z));
        this.barrel8.setRotation(VectorTool.combineRotationsBarrel(1, this));

        Vector4f worldPositionBarrel9 = transformPosition(transformB, -0.3659375f + i, 0.244375f - 2 * i, -0.44625f);
        this.barrel9.center().set(new Vector3f(worldPositionBarrel9.x, worldPositionBarrel9.y, worldPositionBarrel9.z));
        this.barrel9.setRotation(VectorTool.combineRotationsBarrel(1, this));

        Vector4f worldPositionBarrel10 = transformPosition(transformB, -0.3659375f + 2 * i, 0.244375f - 2 * i, -0.44625f);
        this.barrel10.center().set(new Vector3f(worldPositionBarrel10.x, worldPositionBarrel10.y, worldPositionBarrel10.z));
        this.barrel10.setRotation(VectorTool.combineRotationsBarrel(1, this));

        Vector4f worldPositionBarrel11 = transformPosition(transformB, -0.3659375f + 3 * i, 0.244375f - 2 * i, -0.44625f);
        this.barrel11.center().set(new Vector3f(worldPositionBarrel11.x, worldPositionBarrel11.y, worldPositionBarrel11.z));
        this.barrel11.setRotation(VectorTool.combineRotationsBarrel(1, this));
    }
}
