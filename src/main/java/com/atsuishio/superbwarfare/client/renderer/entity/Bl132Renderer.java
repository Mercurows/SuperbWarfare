package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Bl132Model;
import com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Bl132Renderer extends VehicleRenderer<Bl132Entity> {
    public Bl132Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Bl132Model());
    }
}
