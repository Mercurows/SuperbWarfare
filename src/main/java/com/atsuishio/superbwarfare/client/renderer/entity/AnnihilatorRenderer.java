package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.*;
import com.atsuishio.superbwarfare.client.model.entity.AnnihilatorModel;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class AnnihilatorRenderer extends VehicleRenderer<AnnihilatorEntity> {
    public AnnihilatorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AnnihilatorModel());
        this.addRenderLayer(new AnnihilatorLayer(this));
        this.addRenderLayer(new AnnihilatorGlowLayer(this));
        this.addRenderLayer(new AnnihilatorPowerLayer(this));
        this.addRenderLayer(new AnnihilatorPowerLightLayer(this));
        this.addRenderLayer(new AnnihilatorLedLayer(this));
        this.addRenderLayer(new AnnihilatorLedLightLayer(this));
    }
}
