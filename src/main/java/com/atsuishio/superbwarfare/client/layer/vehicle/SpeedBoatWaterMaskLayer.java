package com.atsuishio.superbwarfare.client.layer.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class SpeedBoatWaterMaskLayer extends GeoRenderLayer<SpeedboatEntity> {

    public SpeedBoatWaterMaskLayer(GeoRenderer<SpeedboatEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack, SpeedboatEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        // 这玩意到底为什么能生效？？？
        getRenderer().reRender(getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, RenderType.waterMask(), bufferSource.getBuffer(RenderType.waterMask()), partialTick, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
