package com.atsuishio.superbwarfare.client.layer.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

import static com.atsuishio.superbwarfare.entity.vehicle.SpeedboatEntity.HEAT;

public class SpeedBoatHeatLayer extends GeoRenderLayer<SpeedboatEntity> {
    private static final ResourceLocation LAYER = Mod.loc("textures/entity/speedboat_heat.png");

    public SpeedBoatHeatLayer(GeoRenderer<SpeedboatEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack, SpeedboatEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType glowRenderType = RenderType.eyes(LAYER);
        float heat = animatable.getEntityData().get(HEAT) < 20 ? 0 : animatable.getEntityData().get(HEAT) - 20;
        int heatColor = (int) (heat / 80);
        getRenderer().reRender(getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, glowRenderType, bufferSource.getBuffer(glowRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.color(255, heatColor, heatColor, heatColor)
        );
    }
}

