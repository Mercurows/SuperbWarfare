package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.projectile.RpgRocketStandardLayer;
import com.atsuishio.superbwarfare.client.model.entity.RpgRocketStandardModel;
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketStandardEntity;
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

public class RpgRocketStandardRenderer extends GeoEntityRenderer<RpgRocketStandardEntity> {
    public RpgRocketStandardRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RpgRocketStandardModel());
        this.addRenderLayer(new RpgRocketStandardLayer(this));
    }

    @Override
    public RenderType getRenderType(RpgRocketStandardEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void defaultRender(PoseStack poseStack, RpgRocketStandardEntity animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot()) - 90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90 + Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
        super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
        
        poseStack.popPose();
    }

    @Override
    protected float getDeathMaxRotation(RpgRocketStandardEntity entityLivingBaseIn) {
        return 0;
    }
}
