package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Mle1934Model;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Mle1934Renderer extends VehicleRenderer<Mle1934Entity> {
    public Mle1934Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Mle1934Model());
    }
}
