package com.atsuishio.superbwarfare.client.layer.gun;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.item.gun.shotgun.M870Item;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class M870Layer extends GeoRenderLayer<M870Item> {
    private static final ResourceLocation LAYER = ModUtils.loc("textures/item/m870_e.png");

    public M870Layer(GeoRenderer<M870Item> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack, M870Item animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType glowRenderType = RenderType.eyes(LAYER);
        getRenderer().reRender(getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, glowRenderType, bufferSource.getBuffer(glowRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
    }
}
