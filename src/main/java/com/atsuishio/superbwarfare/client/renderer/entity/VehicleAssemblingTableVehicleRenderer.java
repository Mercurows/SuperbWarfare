package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.VehicleAssemblingTableVehicleModel;
import com.atsuishio.superbwarfare.entity.vehicle.VehicleAssemblingTableVehicleEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class VehicleAssemblingTableVehicleRenderer extends VehicleRenderer<VehicleAssemblingTableVehicleEntity> {

    public VehicleAssemblingTableVehicleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new VehicleAssemblingTableVehicleModel());
        this.shadowRadius = 0.5f;
    }

}
