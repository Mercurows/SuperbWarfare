package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.TowModel;
import com.atsuishio.superbwarfare.entity.vehicle.TowEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class TowRenderer extends VehicleRenderer<TowEntity> {
    public TowRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TowModel());
    }
}
