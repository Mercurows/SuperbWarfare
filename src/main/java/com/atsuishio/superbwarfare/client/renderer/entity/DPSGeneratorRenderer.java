package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.DPSGeneratorLayer;
import com.atsuishio.superbwarfare.client.model.entity.DPSGeneratorModel;
import com.atsuishio.superbwarfare.entity.DPSGeneratorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
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
    public void preRender(PoseStack poseStack, DPSGeneratorEntity animatable, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }

    @Override
    protected float getDeathMaxRotation(DPSGeneratorEntity entityLivingBaseIn) {
        return 0.0F;
    }

    @Override
    public boolean shouldShowName(DPSGeneratorEntity animatable) {
        return animatable.hasCustomName();
    }
}
