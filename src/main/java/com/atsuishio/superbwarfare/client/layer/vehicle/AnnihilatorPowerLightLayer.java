package com.atsuishio.superbwarfare.client.layer.vehicle;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class AnnihilatorPowerLightLayer extends GeoRenderLayer<AnnihilatorEntity> {

    private static final ResourceLocation LAYER = Mod.loc("textures/entity/annihilator_power.png");

    public AnnihilatorPowerLightLayer(GeoRenderer<AnnihilatorEntity> entityRenderer) {
        super(entityRenderer);
    }

    @Override
    public void render(PoseStack poseStack, AnnihilatorEntity animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        RenderType glowRenderType = RenderType.eyes(LAYER);
        float red = 1 - Mth.clamp(2.5f * animatable.getEnergy() / animatable.getMaxEnergy(), 0, 1);
        float green = Mth.clamp(2.5f * animatable.getEnergy() / animatable.getMaxEnergy(), 0, 1);
        getRenderer().reRender(getDefaultBakedModel(animatable), poseStack, bufferSource, animatable, glowRenderType, bufferSource.getBuffer(glowRenderType), partialTick, packedLight, OverlayTexture.NO_OVERLAY,
                FastColor.ARGB32.color(255, (int) (red * 255), (int) (green * 255), 0));
    }
}
