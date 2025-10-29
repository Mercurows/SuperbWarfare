package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.WheelChairModel;
import com.atsuishio.superbwarfare.entity.vehicle.WheelChairEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class WheelChairRenderer extends VehicleRenderer<WheelChairEntity> {

    public WheelChairRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WheelChairModel());
        this.shadowRadius = 0.5f;
    }
}
