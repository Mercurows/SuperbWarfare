package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Tom6Model;
import com.atsuishio.superbwarfare.entity.vehicle.Tom6Entity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Tom6Renderer extends GeoEntityRenderer<Tom6Entity> {

    public Tom6Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Tom6Model());
        this.shadowRadius = 0.5f;
    }
}
