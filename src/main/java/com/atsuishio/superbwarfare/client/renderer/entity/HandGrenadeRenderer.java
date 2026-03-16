package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.HandGrenadeEntityModel;
import com.atsuishio.superbwarfare.entity.projectile.HandGrenadeEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class HandGrenadeRenderer extends GeoEntityRenderer<HandGrenadeEntity> {
    public HandGrenadeRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new HandGrenadeEntityModel());
    }

    @Override
    public RenderType getRenderType(HandGrenadeEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void defaultRender(PoseStack poseStack, HandGrenadeEntity entityIn, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTicks, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0, entityIn.getBbHeight() / 2, 0);
        poseStack.mulPose(Axis.YP.rotationDegrees(-yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
        super.defaultRender(poseStack, entityIn, bufferSource, renderType, buffer, yaw, partialTicks, packedLight);
        poseStack.popPose();
    }

    @Override
    protected float getDeathMaxRotation(HandGrenadeEntity entityLivingBaseIn) {
        return 0;
    }
}
