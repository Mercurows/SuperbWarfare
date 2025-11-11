package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Mk42Model;
import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Mk42Renderer extends VehicleRenderer<Mk42Entity> {

    public Mk42Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Mk42Model());
    }
}
