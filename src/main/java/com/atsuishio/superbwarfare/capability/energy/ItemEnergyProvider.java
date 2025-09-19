package com.atsuishio.superbwarfare.capability.energy;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemEnergyProvider implements ICapabilityProvider {

    private final LazyOptional<IEnergyStorage> capability;

    public ItemEnergyProvider(ItemStack stack, int energyCapacity) {
        this(stack, LazyOptional.of(() -> new ItemEnergyStorage(stack, energyCapacity)));
    }

    public ItemEnergyProvider(ItemStack stack, LazyOptional<IEnergyStorage> provider) {
        this.capability = provider;
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction dire) {
        return ForgeCapabilities.ENERGY.orEmpty(cap, capability);
    }
}
