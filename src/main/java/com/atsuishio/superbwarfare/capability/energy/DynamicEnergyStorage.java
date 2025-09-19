package com.atsuishio.superbwarfare.capability.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

import java.util.function.Supplier;

public class DynamicEnergyStorage extends EnergyStorage {

    protected final Supplier<Integer> maxStorageGetter;
    protected final Supplier<Integer> maxReceiveGetter;
    protected final Supplier<Integer> maxExtractGetter;

    public DynamicEnergyStorage(Supplier<Integer> maxStorageGetter) {
        this(maxStorageGetter, maxStorageGetter, maxStorageGetter);
    }

    public DynamicEnergyStorage(Supplier<Integer> maxStorageGetter, Supplier<Integer> maxReceiveGetter, Supplier<Integer> maxExtractGetter) {
        super(Integer.MAX_VALUE);

        this.maxStorageGetter = maxStorageGetter;
        this.maxReceiveGetter = maxReceiveGetter;
        this.maxExtractGetter = maxExtractGetter;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        updateProps();
        return super.extractEnergy(maxExtract, simulate);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        updateProps();
        return super.receiveEnergy(maxReceive, simulate);
    }

    @Override
    public boolean canReceive() {
        updateProps();
        return super.canReceive();
    }

    @Override
    public boolean canExtract() {
        updateProps();
        return super.canExtract();
    }

    @Override
    public int getMaxEnergyStored() {
        updateProps();
        return super.getMaxEnergyStored();
    }

    protected void updateProps() {
        this.capacity = maxStorageGetter.get();
        this.maxExtract = maxExtractGetter.get();
        this.maxReceive = maxReceiveGetter.get();
    }
}
