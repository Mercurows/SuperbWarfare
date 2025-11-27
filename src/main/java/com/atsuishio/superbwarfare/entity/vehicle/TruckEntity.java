package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
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
import org.jetbrains.annotations.NotNull;

public class TruckEntity extends GeoVehicleEntity {

    public static final EntityDataAccessor<Boolean> GREEN = SynchedEntityData.defineId(TruckEntity.class, EntityDataSerializers.BOOLEAN);

    public TruckEntity(EntityType<TruckEntity> type, Level world) {
        super(type, world);
    }

    @Override
    public DamageModifier getDamageModifier() {
        return super.getDamageModifier()
                .custom((source, damage) -> getSourceAngle(source, 0.25f) * damage);
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
    public void baseTick() {
        super.baseTick();
        if (decoyInputDown()) {
            horn();
        }
    }
}
