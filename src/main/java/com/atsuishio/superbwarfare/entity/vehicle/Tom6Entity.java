package com.atsuishio.superbwarfare.entity.vehicle;

import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData;
import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.data.vehicle.subdata.DestroyInfo;
import com.atsuishio.superbwarfare.entity.projectile.MelonBombEntity;
import com.atsuishio.superbwarfare.entity.vehicle.base.GeoVehicleEntity;
import com.atsuishio.superbwarfare.init.ModEntities;
import com.atsuishio.superbwarfare.tools.ParticleTool;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.PlayMessages;
import org.jetbrains.annotations.NotNull;
import org.joml.Math;
import org.joml.Matrix4d;
import org.joml.Vector4d;

public class Tom6Entity extends GeoVehicleEntity {

    public static final EntityDataAccessor<Boolean> MELON = SynchedEntityData.defineId(Tom6Entity.class, EntityDataSerializers.BOOLEAN);


    public Tom6Entity(PlayMessages.SpawnEntity packet, Level world) {
        this(ModEntities.TOM_6.get(), world);
    }

    public Tom6Entity(EntityType<Tom6Entity> type, Level world) {
        super(type, world);
    }

    @Override
    public DefaultVehicleData computeProperties(VehicleData vehicleData, DefaultVehicleData rawData) {
        if (this.entityData.get(MELON)) {
            rawData.destroyInfo = new DestroyInfo(
                    rawData.destroyInfo.crashPassengers,
                    rawData.destroyInfo.explodePassengers,
                    rawData.destroyInfo.explodeBlocks,
                    getMelonExplosionDamage(),
                    getMelonExplosionRadius(),
                    ParticleTool.ParticleType.HUGE
            );
        }
        return super.computeProperties(vehicleData, rawData);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MELON, false);
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Melon", this.entityData.get(MELON));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(MELON, compound.getBoolean("Melon"));
    }

    @Override
    public @NotNull InteractionResult interact(Player player, @NotNull InteractionHand hand) {
        if (player.getMainHandItem().is(Items.MELON) && !entityData.get(MELON)) {
            entityData.set(MELON, true);
            player.getMainHandItem().shrink(1);
            player.level().playSound(player, this.getOnPos(), SoundEvents.WOOD_PLACE, SoundSource.PLAYERS, 1, 1);
            return InteractionResult.SUCCESS;
        }
        return super.interact(player, hand);
    }

    @Override
    public void baseTick() {
        super.baseTick();
        Entity passenger = getFirstPassenger();
        // 空格投掷西瓜炸弹
        if (upInputDown() && !onGround() && entityData.get(MELON) && passenger instanceof Player player) {
            entityData.set(MELON, false);

            Matrix4d transform = getVehicleTransform(1);
            Vector4d worldPosition;
            worldPosition = transformPosition(transform, 0, 0.3, 0);

            MelonBombEntity melonBomb = new MelonBombEntity(player, player.level());
            melonBomb.setExplosionDamage(getMelonExplosionDamage());
            melonBomb.setExplosionRadius(getMelonExplosionRadius());
            melonBomb.setPos(worldPosition.x, worldPosition.y, worldPosition.z);
            melonBomb.shoot(getDeltaMovement().x, getDeltaMovement().y, getDeltaMovement().z, (float) getDeltaMovement().length(), 0);
            passenger.level().addFreshEntity(melonBomb);

            this.level().playSound(null, getOnPos(), SoundEvents.IRON_DOOR_OPEN, SoundSource.PLAYERS, 1, 1);
            setUpInputDown(false);
        }
    }

    public float getMelonExplosionDamage() {
        var gunData = getGunData("MelonBomb");
        if (gunData != null) {
            return (float) gunData.compute().explosionDamage;
        } else {
            return 0;
        }
    }

    public float getMelonExplosionRadius() {
        var gunData = getGunData("MelonBomb");
        if (gunData != null) {
            return (float) gunData.compute().explosionRadius;
        } else {
            return 0;
        }
    }

    @Override
    public boolean engineRunning() {
        return (getFirstPassenger() != null && Math.abs(getDeltaMovement().length()) > 0);
    }

    @Override
    public float getEngineSoundVolume() {
        return (float) getDeltaMovement().length();
    }
}
