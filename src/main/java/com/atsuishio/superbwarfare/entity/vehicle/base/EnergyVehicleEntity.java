package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.capability.energy.SyncedEntityEnergyStorage;
import com.atsuishio.superbwarfare.capability.energy.VehicleEnergyStorage;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class EnergyVehicleEntity extends VehicleEntity {

    public static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(EnergyVehicleEntity.class, EntityDataSerializers.INT);

    protected final SyncedEntityEnergyStorage energyStorage = new VehicleEnergyStorage(this);
    protected final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> energyStorage);

    public EnergyVehicleEntity(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setEnergy(0);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ENERGY, 0);
    }

    public EntityDataAccessor<Integer> getEnergyDataAccessor() {
        return ENERGY;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        if (compound.get("Energy") instanceof IntTag energyNBT) {
            energyStorage.deserializeNBT(energyNBT);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        energy.invalidate();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.put("Energy", energyStorage.serializeNBT());
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

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return this.getCapability(cap, null);
    }
}
