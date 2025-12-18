package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.DPSGeneratorLayer;
import com.atsuishio.superbwarfare.client.model.entity.DPSGeneratorModel;
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DPSGeneratorRenderer extends GeoEntityRenderer<DPSGeneratorEntity> {

    public DPSGeneratorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new DPSGeneratorModel());
        this.shadowRadius = 0f;
        this.addRenderLayer(new DPSGeneratorLayer(this));
    }

    @Override
    public RenderType getRenderType(DPSGeneratorEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    protected float getDeathMaxRotation(DPSGeneratorEntity entityLivingBaseIn) {
        return 0;
    }

    @Override
    public boolean shouldShowName(DPSGeneratorEntity animatable) {
        return animatable.hasCustomName();
    }
}
