package com.atsuishio.superbwarfare.capability.energy;

import com.atsuishio.superbwarfare.component.ModDataComponents;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class ItemEnergyStorage extends DynamicEnergyStorage {

    private final ItemStack stack;

    public ItemEnergyStorage(ItemStack stack, int capacity) {
        this(stack, capacity, capacity, capacity);
    }

    public ItemEnergyStorage(ItemStack stack, int capacity, int maxReceive, int maxExtract) {
        this(stack, s -> capacity, s -> maxReceive, s -> maxExtract);
    }

    public ItemEnergyStorage(ItemStack stack, Function<ItemStack, Integer> capacityGetter, Function<ItemStack, Integer> maxReceiveGetter, Function<ItemStack, Integer> maxExtractGetter) {
        super(() -> capacityGetter.apply(stack), () -> maxReceiveGetter.apply(stack), () -> maxExtractGetter.apply(stack));

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
