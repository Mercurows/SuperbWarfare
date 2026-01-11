package com.atsuishio.superbwarfare.client.layer.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.TinySpeedboatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class TinySpeedBoatWaterMaskLayer extends GeoRenderLayer<TinySpeedboatEntity> {

    public TinySpeedBoatWaterMaskLayer(GeoRenderer<TinySpeedboatEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack, TinySpeedboatEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        getRenderer().reRender(getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, RenderType.waterMask(), bufferSource.getBuffer(RenderType.waterMask()), partialTick, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
