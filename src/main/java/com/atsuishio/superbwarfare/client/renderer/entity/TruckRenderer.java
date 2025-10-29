package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.TruckModel;
import com.atsuishio.superbwarfare.entity.vehicle.TruckEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class TruckRenderer extends VehicleRenderer<TruckEntity> {

    public TruckRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TruckModel());
//        this.addRenderLayer(new TruckLayer(this));
    }
}
