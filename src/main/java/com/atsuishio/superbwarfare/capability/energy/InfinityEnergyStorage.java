package com.atsuishio.superbwarfare.capability.energy;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 无限供电能力，纯逆天
 */
public class InfinityEnergyStorage implements IEnergyStorage {
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        return 0;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        return maxExtract;
    }

    @Override
    public int getEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxEnergyStored() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
