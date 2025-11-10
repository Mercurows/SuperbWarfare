package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.projectile.RpgRocketTBGLayer;
import com.atsuishio.superbwarfare.client.model.entity.RpgRocketTBGModel;
import com.atsuishio.superbwarfare.entity.projectile.RpgRocketTBGEntity;
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

public class RpgRocketTBGRenderer extends GeoEntityRenderer<RpgRocketTBGEntity> {
    public RpgRocketTBGRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new RpgRocketTBGModel());
        this.addRenderLayer(new RpgRocketTBGLayer(this));
    }

    @Override
    public RenderType getRenderType(RpgRocketTBGEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void defaultRender(PoseStack poseStack, RpgRocketTBGEntity animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot()) - 90));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90 + Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
        super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
        
        poseStack.popPose();
    }

    @Override
    protected float getDeathMaxRotation(RpgRocketTBGEntity entityLivingBaseIn) {
        return 0;
    }
}
