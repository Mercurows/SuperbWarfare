package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Ah6Model;
import com.atsuishio.superbwarfare.entity.vehicle.Ah6Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Ah6Renderer extends VehicleRenderer<Ah6Entity> {
    public Ah6Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Ah6Model());
        this.shadowRadius = 0.5f;
    }
}
