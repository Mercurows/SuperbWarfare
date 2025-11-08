package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.TargetLayer;
import com.atsuishio.superbwarfare.client.model.entity.TargetModel;
import com.atsuishio.superbwarfare.entity.TargetEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TargetRenderer extends GeoEntityRenderer<TargetEntity> {
    public TargetRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TargetModel());
        this.shadowRadius = 0f;
        this.addRenderLayer(new TargetLayer(this));
    }

    @Override
    public RenderType getRenderType(TargetEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    protected float getDeathMaxRotation(TargetEntity entityLivingBaseIn) {
        return 0;
    }

    @Override
    public boolean shouldShowName(TargetEntity animatable) {
        return animatable.hasCustomName();
    }
}
