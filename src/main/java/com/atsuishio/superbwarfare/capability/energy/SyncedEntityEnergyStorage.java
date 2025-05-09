package com.atsuishio.superbwarfare.capability.energy;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.neoforged.neoforge.energy.EnergyStorage;
import org.jetbrains.annotations.NotNull;

/**
 * 自动同步的实体能量存储能力，会和客户端自动同步实体的当前能量值
 */
public class SyncedEntityEnergyStorage extends EnergyStorage {

    protected SynchedEntityData entityData;
    protected EntityDataAccessor<Integer> energyDataAccessor;

    /**
     * 自动同步的实体能量存储能力
     *
     * @param capacity           能量上限
     * @param data               实体的entityData
     * @param energyDataAccessor 能量的EntityDataAccessor
     */
    public SyncedEntityEnergyStorage(int capacity, SynchedEntityData data, EntityDataAccessor<Integer> energyDataAccessor) {
        this(capacity, capacity, capacity, data, energyDataAccessor);
    }

    public SyncedEntityEnergyStorage(int capacity, int maxReceive, int maxExtract, SynchedEntityData data, EntityDataAccessor<Integer> energyDataAccessor) {
        super(capacity, maxReceive, maxExtract, 0);

        this.entityData = data;
        this.energyDataAccessor = energyDataAccessor;
    }

    public void setEnergy(int energy) {
        this.energy = energy;
        entityData.set(energyDataAccessor, energy);
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public void setMaxExtract(int maxExtract) {
        this.maxExtract = maxExtract;
    }

    public void setMaxReceive(int maxReceive) {
        this.maxReceive = maxReceive;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        var received = super.receiveEnergy(maxReceive, simulate);

        if (!simulate) {
            entityData.set(energyDataAccessor, this.energy);
        }

        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        var extracted = super.extractEnergy(maxExtract, simulate);

        if (!simulate) {
            entityData.set(energyDataAccessor, energy);
        }

        return extracted;
    }

    @Override
    public int getEnergyStored() {
        // 获取同步数据，保证客户端能正确获得能量值
        return entityData.get(energyDataAccessor);
    }

    @Override
    public void deserializeNBT(HolderLookup.@NotNull Provider provider, @NotNull Tag nbt) {
        super.deserializeNBT(provider, nbt);
        entityData.set(energyDataAccessor, energy);
    }
}
