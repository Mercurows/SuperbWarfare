package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.Bmp2Layer;
import com.atsuishio.superbwarfare.client.model.entity.Bmp2Model;
import com.atsuishio.superbwarfare.entity.vehicle.Bmp2Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Bmp2Renderer extends VehicleRenderer<Bmp2Entity> {

    public Bmp2Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Bmp2Model());
        this.addRenderLayer(new Bmp2Layer(this));
    }
}
