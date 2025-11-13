package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.Hpj11HeatLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.Hpj11Layer;
import com.atsuishio.superbwarfare.client.model.entity.Hpj11Model;
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Hpj11Renderer extends VehicleRenderer<Hpj11Entity> {

    public Hpj11Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Hpj11Model());
        this.addRenderLayer(new Hpj11Layer(this));
        this.addRenderLayer(new Hpj11HeatLayer(this));
    }
}
