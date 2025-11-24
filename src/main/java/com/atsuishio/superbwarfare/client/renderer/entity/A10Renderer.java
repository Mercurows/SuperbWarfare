package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.A10Model;
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class A10Renderer extends VehicleRenderer<A10Entity> {
    public A10Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new A10Model());
        this.shadowRadius = 0.5f;
    }
}
