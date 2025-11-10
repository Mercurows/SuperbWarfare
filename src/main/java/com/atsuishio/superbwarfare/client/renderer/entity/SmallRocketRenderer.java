package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.projectile.SmallRocketLayer;
import com.atsuishio.superbwarfare.client.model.entity.SmallRocketModel;
import com.atsuishio.superbwarfare.entity.projectile.SmallRocketEntity;
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

public class SmallRocketRenderer extends GeoEntityRenderer<SmallRocketEntity> {
    public SmallRocketRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SmallRocketModel());
        this.addRenderLayer(new SmallRocketLayer(this));
    }

    @Override
    public RenderType getRenderType(SmallRocketEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void defaultRender(PoseStack poseStack, SmallRocketEntity entityIn, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entityIn.yRotO, entityIn.getYRot()) - 90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90 + Mth.lerp(partialTick, entityIn.xRotO, entityIn.getXRot())));
        super.defaultRender(poseStack, entityIn, bufferSource, renderType, buffer, yaw, partialTick, packedLight);

        poseStack.popPose();
    }

    @Override
    protected float getDeathMaxRotation(SmallRocketEntity entityLivingBaseIn) {
        return 0;
    }
}
