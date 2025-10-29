package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.PrismTankLaserLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.PrismTankLightLayer;
import com.atsuishio.superbwarfare.client.model.entity.PrismTankModel;
import com.atsuishio.superbwarfare.entity.vehicle.PrismTankEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class PrismTankRenderer extends VehicleRenderer<PrismTankEntity> {

    public PrismTankRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new PrismTankModel());
        this.addRenderLayer(new PrismTankLaserLayer(this));
        this.addRenderLayer(new PrismTankLightLayer(this));
    }
}
