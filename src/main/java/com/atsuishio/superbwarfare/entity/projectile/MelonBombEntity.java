package com.atsuishio.superbwarfare.entity.projectile;

import com.atsuishio.superbwarfare.config.server.VehicleConfig;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.tools.ProjectileTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrownPotion;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

// TODO 配置文件
public class MelonBombEntity extends FastThrowableProjectile implements DestroyableProjectileEntity, AerialBombEntity {
    public static final EntityDataAccessor<Float> HEALTH = SynchedEntityData.defineId(MelonBombEntity.class, EntityDataSerializers.FLOAT);

    public MelonBombEntity(EntityType<? extends MelonBombEntity> type, Level world) {
        super(type, world);
        this.noCulling = true;
    }

    public MelonBombEntity(LivingEntity entity, Level level) {
        super(ModEntities.MELON_BOMB.get(), entity, level);
    }

    protected @NotNull Item getDefaultItem() {
        return Items.MELON;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double pDistance) {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.getDirectEntity() instanceof ThrownPotion || source.getDirectEntity() instanceof AreaEffectCloud)
            return false;
        if (source.is(DamageTypes.FALL))
            return false;
        if (source.is(DamageTypes.CACTUS))
            return false;
        if (source.is(DamageTypes.DROWN))
            return false;
        if (source.is(DamageTypes.DRAGON_BREATH))
            return false;
        if (source.is(DamageTypes.WITHER))
            return false;
        if (source.is(DamageTypes.WITHER_SKULL))
            return false;
        this.entityData.set(HEALTH, this.entityData.get(HEALTH) - amount);

        return super.hurt(source, amount);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HEALTH, 10F);
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

    @Override
    public void onHitBlock(@NotNull BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        ProjectileTool.causeCustomExplode(this, VehicleConfig.TOM_6_BOMB_EXPLOSION_DAMAGE.get(), VehicleConfig.TOM_6_BOMB_EXPLOSION_RADIUS.get().floatValue(), 1.5f);
        this.discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (tickCount > 600 || this.entityData.get(HEALTH) <= 0) {
            this.discard();
            if (!this.level().isClientSide) {
                ProjectileTool.causeCustomExplode(this, VehicleConfig.TOM_6_BOMB_EXPLOSION_DAMAGE.get(), VehicleConfig.TOM_6_BOMB_EXPLOSION_RADIUS.get().floatValue(), 1.5f);
            }
        }
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05F;
    }

    @Override
    public void setDamage(float damage) {

    }

    @Override
    public void setExplosionDamage(float damage) {

    }

    @Override
    public void setExplosionRadius(float radius) {

    }
}
