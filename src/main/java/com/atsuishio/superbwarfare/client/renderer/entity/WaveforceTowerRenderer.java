package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.WaveforceTowerGlowLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.WaveforceTowerLaserLayer;
import com.atsuishio.superbwarfare.client.model.entity.WaveforceTowerModel;
import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class WaveforceTowerRenderer extends VehicleRenderer<WaveforceTowerEntity> {
    public WaveforceTowerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WaveforceTowerModel());
        this.addRenderLayer(new WaveforceTowerGlowLayer(this));
        this.addRenderLayer(new WaveforceTowerLaserLayer(this));
    }
}
