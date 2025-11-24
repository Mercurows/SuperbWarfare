package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Mi28Model;
import com.atsuishio.superbwarfare.entity.vehicle.Mi28Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Mi28Renderer extends VehicleRenderer<Mi28Entity> {
    public Mi28Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Mi28Model());
        this.shadowRadius = 1f;
    }
}
