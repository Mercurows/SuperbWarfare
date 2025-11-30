package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.LavAdLayer;
import com.atsuishio.superbwarfare.client.model.entity.LavAdModel;
import com.atsuishio.superbwarfare.entity.vehicle.LavAdEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class LavAdRenderer extends VehicleRenderer<LavAdEntity> {
    public LavAdRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LavAdModel());
        this.addRenderLayer(new LavAdLayer(this));
    }
}
