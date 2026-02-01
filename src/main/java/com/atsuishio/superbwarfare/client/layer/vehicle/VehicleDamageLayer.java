package com.atsuishio.superbwarfare.client.layer.vehicle;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class VehicleDamageLayer extends GeoRenderLayer<GeoAnimatable> {
    public VehicleDamageLayer(GeoRenderer<GeoAnimatable> entityRenderer) {
        super(entityRenderer);
    }

    public void render(PoseStack poseStack, GeoAnimatable animatable, BakedGeoModel bakedModel, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight, int packedOverlay) {
        if (animatable instanceof VehicleEntity vehicle) {
            ResourceLocation texture = getRenderer().getTextureLocation(animatable);
            RenderType blackRender = RenderType.entityTranslucentEmissive(texture);
            var healthRatio = vehicle.getHealth() / vehicle.getMaxHealth();
            float color = adjustColorOnHealth(healthRatio);
            getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, blackRender, bufferSource.getBuffer(blackRender), partialTick, packedLight, packedOverlay, 0xFFFFFFFF);
        }
    }

    private static float adjustColorOnHealth(float healthRatio) {
        if (healthRatio > 0.4f || (ClientEventHandler.activeThermalImaging)) return 1f;
        return (float) Math.max(2.5 * healthRatio, 0.15);
    }
}