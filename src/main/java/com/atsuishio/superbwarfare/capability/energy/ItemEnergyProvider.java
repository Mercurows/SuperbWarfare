package com.atsuishio.superbwarfare.capability.energy;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;

public class ItemEnergyProvider implements ICapabilityProvider<ItemStack, Void, IEnergyStorage> {

    public ItemEnergyProvider(ItemStack stack, int energyCapacity) {
    }

    @Override
    public @org.jetbrains.annotations.Nullable IEnergyStorage getCapability(@NotNull ItemStack object, Void context) {
        return object.getCapability(Capabilities.EnergyStorage.ITEM);
    }
}
