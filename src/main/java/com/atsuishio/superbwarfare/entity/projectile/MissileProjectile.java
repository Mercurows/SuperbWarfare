package com.atsuishio.superbwarfare.entity.projectile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public abstract class MissileProjectile extends DestroyableProjectile implements CustomSyncMotionEntity, IEntityWithComplexSpawn {

    public static final EntityDataAccessor<String> TARGET_UUID = SynchedEntityData.defineId(MissileProjectile.class, EntityDataSerializers.STRING);

    public Vec3 targetPos;
    public int guideType = 0;
    public boolean distracted = false;
    public boolean lost = false;
    public boolean lostTarget = false;

    public MissileProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public MissileProjectile(EntityType<? extends ThrowableItemProjectile> pEntityType, @Nullable Entity pShooter, Level pLevel) {
        super(pEntityType, pLevel);
        this.setOwner(pShooter);
        if (pShooter != null) {
            this.setPos(pShooter.getX(), pShooter.getEyeY() - (double) 0.1F, pShooter.getZ());
        }
    }

    public void setTargetUuid(String uuid) {
        this.entityData.set(TARGET_UUID, uuid);
    }

    public void setGuideType(int guideType) {
        this.guideType = guideType;
    }

    public void setTargetVec(Vec3 targetPos) {
        if (targetPos != null) {
            this.targetPos = targetPos;
        }
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TARGET_UUID, "none");
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("TargetUuid")) {
            this.entityData.set(TARGET_UUID, compound.getString("TargetUuid"));
        }
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putString("TargetUuid", this.entityData.get(TARGET_UUID));
    }

    @Override
    protected void updateRotation() {
    }

    public void turn(Vec3 vec3, float turnSpeed) {
        Vec3 v0 = getDeltaMovement().normalize();

        vec3 = vec3.add(v0.scale(-0.4));

        double d0 = vec3.horizontalDistance();
        float targetAngleY = (float) (-Mth.atan2(vec3.x, vec3.z) * (double) (180F / (float) Math.PI));
        float targetAngleX = (float) (-Mth.atan2(vec3.y, d0) * (double) (180F / (float) Math.PI));

        float diffY = Mth.wrapDegrees(targetAngleY - this.getYRot());
        float diffX = Mth.wrapDegrees(targetAngleX - this.getXRot());

        this.setYRot(this.getYRot() + Mth.clamp(0.95f * diffY, -turnSpeed, turnSpeed));
        this.setXRot(this.getXRot() + Mth.clamp(0.95f * diffX, -turnSpeed, turnSpeed));
    }

    @Override
    public boolean shouldSyncMotion() {
        return true;
    }

    @Override
    public boolean forceLoadChunk() {
        return true;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public double getDefaultGravity() {
        return 0;
    }
}
