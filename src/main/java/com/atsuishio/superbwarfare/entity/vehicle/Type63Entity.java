package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.config.server.ExplosionConfig;
import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ContainerMobileVehicleEntity;
import com.atsuishio.superbwarfare.init.ModDamageTypes;
import com.atsuishio.superbwarfare.init.ModSerializers;
import com.atsuishio.superbwarfare.init.ModSounds;
import com.atsuishio.superbwarfare.init.ModTags;
import com.atsuishio.superbwarfare.item.ContainerBlockItem;
import com.atsuishio.superbwarfare.tools.CustomExplosion;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import com.atsuishio.superbwarfare.tools.VectorTool;
import com.mojang.math.Axis;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.*;
import org.joml.Math;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public class Type63Entity extends ContainerMobileVehicleEntity implements GeoEntity, OBBEntity {

    public static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(Type63Entity.class, EntityDataSerializers.FLOAT);
    public static final EntityDataAccessor<List<Integer>> LOADED_AMMO = SynchedEntityData.defineId(Type63Entity.class, ModSerializers.INT_LIST_SERIALIZER.get());
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
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        var list = new ArrayList<Integer>();
        for (int i = 0; i < this.getContainerSize(); i++) {
            list.add(0);
        }

        builder.define(PITCH, 0F)
                .define(YAW, 0F)
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
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            if (OBB.getLookingObb(player, player.entityInteractionRange()) == hoe1) {
                if (player.level() instanceof ServerLevel) {
                    setYRot(getYRot() + (float) interactionTick);
                    interactEvent(new Vec3(hoe1.center()));
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
            if (OBB.getLookingObb(player, player.entityInteractionRange()) == hoe2) {
                if (player.level() instanceof ServerLevel) {
                    setYRot(getYRot() - (float) interactionTick);
                    interactEvent(new Vec3(hoe2.center()));
                }
                player.swing(InteractionHand.MAIN_HAND);
            }
            if (OBB.getLookingObb(player, player.entityInteractionRange()) == yawController) {
                interactEvent(new Vec3(yawController.center()));
                entityData.set(YAW, Mth.clamp(entityData.get(YAW) + (player.isShiftKeyDown() ? -0.02f : 0.02f) * (float) interactionTick, -15, 15));
                player.swing(InteractionHand.MAIN_HAND);
            }
            if (OBB.getLookingObb(player, player.entityInteractionRange()) == pitchController) {
                interactEvent(new Vec3(pitchController.center()));
                entityData.set(PITCH, Mth.clamp(entityData.get(PITCH) + (player.isShiftKeyDown() ? 0.02f : -0.02f) * (float) interactionTick, -60, 5));
                player.swing(InteractionHand.MAIN_HAND);
            }
        }

        if (player.isShiftKeyDown() && stack.is(ModTags.Items.CROWBAR) && this.getPassengers().isEmpty()) {
            ItemStack container = ContainerBlockItem.createInstance(this);
            if (!player.addItem(container)) {
                player.drop(container, false);
            }
            this.remove(RemovalReason.DISCARDED);
            this.discard();
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public void interactEvent(Vec3 vec3) {
        if (level() instanceof ServerLevel serverLevel) {
            interactionTick++;
            if (tickCount % 5 == 0) {
                serverLevel.playSound(null, vec3.x, vec3.y, vec3.z, ModSounds.HAND_WHEEL_ROT.get(), SoundSource.PLAYERS, 1f, random.nextFloat() * 0.1f + 0.9f);
            }
        }
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
            EventHooks.onExplosionStart(this.level(), explosion);
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
    public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean canTakeItem(@NotNull Container target, int slot, @NotNull ItemStack stack) {
        return false;
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

    @Override
    public void setChanged() {
        this.entityData.set(LOADED_AMMO, this.items.stream().map(i -> i.isEmpty() ? 0 : 1).toList());
    }

    @Override
    public boolean hasEnergyStorage() {
        return false;
    }
}
