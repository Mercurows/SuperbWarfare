package com.atsuishio.superbwarfare.capability.energy;

import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

// TODO 共用实例问题
// TODO 序列化问题
public class BlockEnergyStorageProvider<T> implements ICapabilityProvider<T, Direction, IEnergyStorage> {

    private final IEnergyStorage energy;

    public BlockEnergyStorageProvider(int maxEnergy) {
        this.energy = new EnergyStorage(maxEnergy);
    }

    @Override
    public @Nullable IEnergyStorage getCapability(@NotNull T object, Direction context) {
        return energy;
    }
}