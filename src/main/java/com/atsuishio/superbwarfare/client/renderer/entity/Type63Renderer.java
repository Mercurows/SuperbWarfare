package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Type63Model;
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class Type63Renderer extends VehicleRenderer<Type63Entity> {
    public Type63Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Type63Model());
        this.shadowRadius = 0.8f;
    }
}
