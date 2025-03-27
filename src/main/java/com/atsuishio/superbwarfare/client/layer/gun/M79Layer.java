package com.atsuishio.superbwarfare.client.layer.gun;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.item.gun.launcher.M79Item;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class M79Layer extends GeoRenderLayer<M79Item> {
    private static final ResourceLocation LAYER = ModUtils.loc("textures/item/m79_e.png");

    public M79Layer(GeoRenderer<M79Item> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack, M79Item animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType glowRenderType = RenderType.eyes(LAYER);
        getRenderer().reRender(getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, glowRenderType, bufferSource.getBuffer(glowRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
