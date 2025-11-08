package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.SenpaiModel;
import com.atsuishio.superbwarfare.entity.SenpaiEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SenpaiRenderer extends GeoEntityRenderer<SenpaiEntity> {
    public SenpaiRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SenpaiModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public RenderType getRenderType(SenpaiEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    protected float getDeathMaxRotation(SenpaiEntity entityLivingBaseIn) {
        return 0;
    }
}
