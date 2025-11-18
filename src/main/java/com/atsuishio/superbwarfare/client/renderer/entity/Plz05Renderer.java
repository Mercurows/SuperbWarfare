package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Plz05Model;
import com.atsuishio.superbwarfare.entity.vehicle.Plz05Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Plz05Renderer extends VehicleRenderer<Plz05Entity> {

    public Plz05Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Plz05Model());
    }
}
