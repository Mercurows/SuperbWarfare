package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Ptkm1rModel;
import com.atsuishio.superbwarfare.entity.Ptkm1rEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Ptkm1rRenderer extends GeoEntityRenderer<Ptkm1rEntity> {

    public Ptkm1rRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Ptkm1rModel());
    }

    @Override
    public RenderType getRenderType(Ptkm1rEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void renderRecursively(PoseStack poseStack, Ptkm1rEntity entityIn, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        String name = bone.getName();
        if (name.equals("body")) {
            bone.setRotY(-animatable.getYRot() * Mth.DEG_TO_RAD);
        }
        if (name.equals("zhu2")) {
            bone.setRotX(0.5f * Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot()) * Mth.DEG_TO_RAD);
        }
        super.renderRecursively(poseStack, entityIn, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
