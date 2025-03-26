package com.atsuishio.superbwarfare.capability.energy;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemEnergyProvider implements ICapabilityProvider<ItemStack, Void, IEnergyStorage> {
    private final EnergyStorage energyStorage;

    public ItemEnergyProvider(int energyCapacity) {
        this.energyStorage = new EnergyStorage(energyCapacity);
    }

    @Override
    public @Nullable IEnergyStorage getCapability(@NotNull ItemStack object, Void context) {
        return energyStorage;
    }
}
