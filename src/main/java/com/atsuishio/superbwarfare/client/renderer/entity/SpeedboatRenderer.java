package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.SpeedBoatHeatLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.SpeedBoatLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.SpeedBoatPowerLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.SpeedBoatWaterMaskLayer;
import com.atsuishio.superbwarfare.client.model.entity.SpeedboatModel;
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class SpeedboatRenderer extends VehicleRenderer<SpeedboatEntity> {

    public SpeedboatRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SpeedboatModel());
        this.addRenderLayer(new SpeedBoatLayer(this));
        this.addRenderLayer(new SpeedBoatWaterMaskLayer(this));
        this.addRenderLayer(new SpeedBoatPowerLayer(this));
        this.addRenderLayer(new SpeedBoatHeatLayer(this));
    }

}
