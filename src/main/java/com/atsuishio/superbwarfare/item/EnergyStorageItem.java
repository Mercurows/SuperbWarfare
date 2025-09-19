package com.atsuishio.superbwarfare.item;

import net.minecraft.world.item.ItemStack;

public interface EnergyStorageItem {
    int getMaxEnergy(ItemStack stack);

    default int getMaxReceiveEnergy(ItemStack stack) {
        return getMaxEnergy(stack);
    }

    default int getMaxExtractEnergy(ItemStack stack) {
        return getMaxEnergy(stack);
    }

}
