package com.atsuishio.superbwarfare.inventory.handler;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraftforge.items.ItemStackHandler;

public class VehicleContainerHandler extends ItemStackHandler {

    private final VehicleEntity vehicle;

    public VehicleContainerHandler(int size, VehicleEntity vehicle) {
        super(size);
        this.vehicle = vehicle;
    }
}
