package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.WaterMaskEntityModel;
import com.atsuishio.superbwarfare.entity.WaterMaskEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class WaterMaskEntityRenderer extends GeoEntityRenderer<WaterMaskEntity> {

    public WaterMaskEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WaterMaskEntityModel());
    }

    @Override
    public RenderType getRenderType(WaterMaskEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.waterMask();
    }
}
