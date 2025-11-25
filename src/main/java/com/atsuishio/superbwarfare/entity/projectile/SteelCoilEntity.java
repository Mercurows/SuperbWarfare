//package com.atsuishio.superbwarfare.entity.projectile;
//
//import com.atsuishio.superbwarfare.Mod;
//import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
//import net.minecraft.nbt.CompoundTag;
//import net.minecraft.network.syncher.EntityDataAccessor;
//import net.minecraft.network.syncher.EntityDataSerializers;
//import net.minecraft.network.syncher.SynchedEntityData;
//import net.minecraft.server.players.OldUsersConverter;
//import net.minecraft.world.entity.EntityType;
//import net.minecraft.world.entity.MoverType;
//import net.minecraft.world.entity.OwnableEntity;
//import net.minecraft.world.level.Level;
//import org.jetbrains.annotations.NotNull;
//import software.bernie.geckolib.animatable.GeoEntity;
//import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
//import software.bernie.geckolib.animation.AnimatableManager;
//import software.bernie.geckolib.util.GeckoLibUtil;
//
//import javax.annotation.Nullable;
//import java.util.Optional;
//import java.util.UUID;
//
//public class SteelCoilEntity extends VehicleEntity implements GeoEntity, OwnableEntity {
//
//    protected static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(SteelCoilEntity.class, EntityDataSerializers.OPTIONAL_UUID);
//    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
//
//    public SteelCoilEntity(EntityType<?> entityType, Level level) {
//        super(entityType, level);
//    }
//
//    @Override
//    protected void defineSynchedData(SynchedEntityData.@NotNull Builder builder) {
//        super.defineSynchedData(builder);
//        builder.define(OWNER_UUID, Optional.empty());
//    }
//
//    @Override
//    public void travel() {
//        super.travel();
//        // TODO 正确实现移动和创人
//        this.move(MoverType.SELF, this.getDeltaMovement());
//    }
//
//    @Override
//    protected void readAdditionalSaveData(@NotNull CompoundTag compound) {
//        UUID uuid;
//        if (compound.hasUUID("Owner")) {
//            uuid = compound.getUUID("Owner");
//        } else {
//            String s = compound.getString("Owner");
//
//            try {
//                if (this.getServer() == null) {
//                    uuid = UUID.fromString(s);
//                } else {
//                    uuid = OldUsersConverter.convertMobOwnerIfNecessary(this.getServer(), s);
//                }
//            } catch (Exception exception) {
//                Mod.LOGGER.error("Couldn't load owner UUID of {}: {}", this, exception);
//                uuid = null;
//            }
//        }
//
//        if (uuid != null) {
//            try {
//                this.setOwnerUUID(uuid);
//            } catch (Throwable ignored) {
//            }
//        }
//    }
//
//    @Override
//    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
//        if (this.getOwnerUUID() != null) {
//            compound.putUUID("Owner", this.getOwnerUUID());
//        }
//    }
//
//    public void setOwnerUUID(@javax.annotation.Nullable UUID pUuid) {
//        this.entityData.set(OWNER_UUID, Optional.ofNullable(pUuid));
//    }
//
//    @Nullable
//    public UUID getOwnerUUID() {
//        return this.entityData.get(OWNER_UUID).orElse(null);
//    }
//
//    @Override
//    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
//    }
//
//    @Override
//    public AnimatableInstanceCache getAnimatableInstanceCache() {
//        return cache;
//    }
//}
