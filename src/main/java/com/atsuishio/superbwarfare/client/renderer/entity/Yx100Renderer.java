package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.Yx100GlowLayer;
import com.atsuishio.superbwarfare.client.model.entity.Yx100Model;
import com.atsuishio.superbwarfare.entity.vehicle.Yx100Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Yx100Renderer extends VehicleRenderer<Yx100Entity> {

    public Yx100Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Yx100Model());
        this.addRenderLayer(new Yx100GlowLayer(this));
    }
}
