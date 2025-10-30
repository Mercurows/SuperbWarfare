package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.Lav150Layer;
import com.atsuishio.superbwarfare.client.model.entity.Lav150Model;
import com.atsuishio.superbwarfare.entity.vehicle.Lav150Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Lav150Renderer extends VehicleRenderer<Lav150Entity> {

    public Lav150Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Lav150Model());
        this.addRenderLayer(new Lav150Layer(this));
    }
}
