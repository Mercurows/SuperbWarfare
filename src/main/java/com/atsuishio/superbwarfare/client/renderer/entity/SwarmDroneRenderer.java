package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.SwarmDroneModel;
import com.atsuishio.superbwarfare.entity.projectile.SwarmDroneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SwarmDroneRenderer extends GeoEntityRenderer<SwarmDroneEntity> {
    public SwarmDroneRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SwarmDroneModel());
    }

    @Override
    public RenderType getRenderType(SwarmDroneEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }
}
