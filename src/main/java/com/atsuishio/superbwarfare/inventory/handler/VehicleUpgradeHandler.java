package com.atsuishio.superbwarfare.inventory.handler;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

public class VehicleUpgradeHandler extends ItemStackHandler {

    private final VehicleEntity vehicle;

    public VehicleUpgradeHandler(int size, VehicleEntity vehicle) {
        super(size);
        this.vehicle = vehicle;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return this.vehicle.isUpgradeValid(slot, stack);
    }
}
