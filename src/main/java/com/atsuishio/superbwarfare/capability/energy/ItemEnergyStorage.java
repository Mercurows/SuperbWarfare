package com.atsuishio.superbwarfare.capability.energy;

import com.atsuishio.superbwarfare.component.ModDataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.EnergyStorage;

public class ItemEnergyStorage extends EnergyStorage {

    private final ItemStack stack;

    public ItemEnergyStorage(ItemStack stack, int capacity) {
        super(capacity, Integer.MAX_VALUE, Integer.MAX_VALUE);

        this.stack = stack;
        var component = stack.get(ModDataComponents.ENERGY);
        this.energy = component == null ? 0 : component;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = super.receiveEnergy(maxReceive, simulate);

        if (received > 0 && !simulate) {
            stack.set(ModDataComponents.ENERGY, getEnergyStored());
        }

        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = super.extractEnergy(maxExtract, simulate);

        if (extracted > 0 && !simulate) {
            stack.set(ModDataComponents.ENERGY, getEnergyStored());
        }

        return extracted;
    }
}
