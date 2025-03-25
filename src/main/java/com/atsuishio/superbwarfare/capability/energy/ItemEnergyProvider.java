package com.atsuishio.superbwarfare.capability.energy;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

public class ItemEnergyProvider implements ICapabilityProvider<ItemStack, Void, IEnergyStorage> {
    private final ItemStack stack;
    private final EnergyStorage energyStorage;

    public ItemEnergyProvider(ItemStack stack, int energyCapacity) {
        this.stack = stack;
        this.energyStorage = new EnergyStorage(energyCapacity);
    }

    @Override
    public @org.jetbrains.annotations.Nullable IEnergyStorage getCapability(@NotNull ItemStack object, Void context) {
        return energyStorage;
    }
}
