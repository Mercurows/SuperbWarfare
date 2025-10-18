package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.entity.IEntityAdditionalSpawnData;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public abstract class DestroyableProjectile extends FastThrowableProjectile implements CustomSyncMotionEntity, IEntityAdditionalSpawnData {

    public static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(DestroyableProjectile.class, EntityDataSerializers.FLOAT);
    private static final DamageModifier DAMAGE_MODIFIER = DamageModifier.createDefaultModifier();
    public DestroyableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public DestroyableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, @Nullable Entity pShooter, Level pLevel) {
        super(pEntityType, pLevel);
        this.setOwner(pShooter);
        if (pShooter != null) {
            this.setPos(pShooter.getX(), pShooter.getEyeY() - (double) 0.1F, pShooter.getZ());
        }
    }

    public DestroyableProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, double pX, double pY, double pZ, Level pLevel) {
        super(pEntityType, pX, pY, pZ, pLevel);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        amount = DAMAGE_MODIFIER.compute(source, amount);
        this.entityData.set(HEALTH, this.entityData.get(HEALTH) - amount);

        return super.hurt(source, amount);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(HEALTH, getMaxHealth());
    }



    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("Health")) {
            this.entityData.set(HEALTH, compound.getFloat("Health"));
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putFloat("Health", this.entityData.get(HEALTH));
    }

    public float getMaxHealth() {
        return 30;
    }
}
