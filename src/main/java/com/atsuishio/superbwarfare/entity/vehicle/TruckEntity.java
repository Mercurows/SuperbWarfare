package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.OBBEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.ThirdPersonCameraPosition;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import com.atsuishio.superbwarfare.event.ClientMouseHandler;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.tools.OBB;
import com.atsuishio.superbwarfare.tools.VectorTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class TruckEntity extends VehicleEntity implements GeoEntity, OBBEntity {

    public static final EntityDataAccessor<Boolean> GREEN = SynchedEntityData.defineId(TruckEntity.class, EntityDataSerializers.BOOLEAN);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OBB obb;
    public OBB obb2;
    public OBB obb3;
    public OBB obb4;
    public OBB obb5;
    public OBB obb6;
    public OBB obb7;

    public TruckEntity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.TRUCK.get(), world);
    }

    public TruckEntity(EntityType<TruckEntity> type, Level world) {
        super(type, world);
        this.obb = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.6875f, 0.6875f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.obb2 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.6875f, 0.6875f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.obb3 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.6875f, 0.6875f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.obb4 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.6875f, 0.6875f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.obb5 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.6875f, 0.6875f), new Quaternionf(), OBB.Part.WHEEL_LEFT);
        this.obb6 = new OBB(this.position().toVector3f(), new Vector3f(0.34375f, 0.6875f, 0.6875f), new Quaternionf(), OBB.Part.WHEEL_RIGHT);
        this.obb7 = new OBB(this.position().toVector3f(), new Vector3f(1.625f, 1.71875f, 5.5f), new Quaternionf(), OBB.Part.BODY);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(GREEN, false);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Green", this.entityData.get(GREEN));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(GREEN, compound.getBoolean("Green"));
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() == Items.LIME_DYE && !this.entityData.get(GREEN)) {
            this.entityData.set(GREEN, true);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.level().playSound(null, this, SoundEvents.BONE_MEAL_USE, this.getSoundSource(), 2, 1);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }

        if (stack.getItem() == Items.RED_DYE && this.entityData.get(GREEN)) {
            this.entityData.set(GREEN, false);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
            this.level().playSound(null, this, SoundEvents.BONE_MEAL_USE, this.getSoundSource(), 2, 1);
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        return super.interact(player, hand);
    }


    @Override
    public ThirdPersonCameraPosition getThirdPersonCameraPosition(int index) {
        return new ThirdPersonCameraPosition(5 + ClientMouseHandler.custom3pDistanceLerp, 1, 0);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        updateOBB();

        lowHealthWarning();

        if (decoyInputDown()) {
            horn();
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
    public List<OBB> getOBBs() {
        return List.of(this.obb, this.obb2, this.obb3, this.obb4, this.obb5, this.obb6, this.obb7);
    }

    @Override
    public void updateOBB() {
        Matrix4f transform = getVehicleTransform(1);

        Vector4f worldPosition = transformPosition(transform, -1.28125f, 0.6875f, 4f);
        this.obb.center().set(new Vector3f(worldPosition.x, worldPosition.y, worldPosition.z));
        this.obb.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition2 = transformPosition(transform, 1.28125f, 0.6875f, 4f);
        this.obb2.center().set(new Vector3f(worldPosition2.x, worldPosition2.y, worldPosition2.z));
        this.obb2.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition3 = transformPosition(transform, -1.28125f, 0.6875f, -1.328125f);
        this.obb3.center().set(new Vector3f(worldPosition3.x, worldPosition3.y, worldPosition3.z));
        this.obb3.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition4 = transformPosition(transform, 1.28125f, 0.6875f, -1.328125f);
        this.obb4.center().set(new Vector3f(worldPosition4.x, worldPosition4.y, worldPosition4.z));
        this.obb4.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition5 = transformPosition(transform, -1.28125f, 0.6875f, -2.828125f);
        this.obb5.center().set(new Vector3f(worldPosition5.x, worldPosition5.y, worldPosition5.z));
        this.obb5.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition6 = transformPosition(transform, 1.28125f, 0.6875f, -2.828125f);
        this.obb6.center().set(new Vector3f(worldPosition6.x, worldPosition6.y, worldPosition6.z));
        this.obb6.setRotation(VectorTool.combineRotations(1, this));

        Vector4f worldPosition7 = transformPosition(transform, 0, 2.34375f, 0.875f);
        this.obb7.center().set(new Vector3f(worldPosition7.x, worldPosition7.y, worldPosition7.z));
        this.obb7.setRotation(VectorTool.combineRotations(1, this));
    }
}
