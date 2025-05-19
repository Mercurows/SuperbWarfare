package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.capability.energy.SyncedEntityEnergyStorage;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.energy.IEnergyStorage;

public abstract class EnergyVehicleEntity extends VehicleEntity {

    public static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(EnergyVehicleEntity.class, EntityDataSerializers.INT);

    // TODO 在数据更新时修改能量相关属性
    protected final IEnergyStorage energyStorage = new SyncedEntityEnergyStorage(this.getMaxEnergy(), this.entityData, ENERGY);

    public EnergyVehicleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setEnergy(0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ENERGY, 0);
    }


    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.get("Energy") instanceof IntTag energyNBT) {
            ((SyncedEntityEnergyStorage) energyStorage).deserializeNBT(level().registryAccess(), energyNBT);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.put("Energy", ((SyncedEntityEnergyStorage) energyStorage).serializeNBT(level().registryAccess()));
    }

    /**
     * 消耗指定电量
     *
     * @param amount 要消耗的电量
     */
    public void consumeEnergy(int amount) {
        this.energyStorage.extractEnergy(amount, false);
    }

    public boolean canConsume(int amount) {
        return this.getEnergy() >= amount;
    }

    public int getEnergy() {
        return this.energyStorage.getEnergyStored();
    }

    public IEnergyStorage getEnergyStorage() {
        return this.energyStorage;
    }

    public void setEnergy(int pEnergy) {
        int targetEnergy = Mth.clamp(pEnergy, 0, this.getMaxEnergy());

        if (targetEnergy > energyStorage.getEnergyStored()) {
            energyStorage.receiveEnergy(targetEnergy - energyStorage.getEnergyStored(), false);
        } else {
            energyStorage.extractEnergy(energyStorage.getEnergyStored() - targetEnergy, false);
        }
    }

    public int getMaxEnergy() {
        return data().maxEnergy();
    }

}
