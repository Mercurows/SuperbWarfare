package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.projectile.SmallCannonShellLayer;
import com.atsuishio.superbwarfare.client.model.entity.SmallCannonShellModel;
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SmallCannonShellRenderer extends GeoEntityRenderer<SmallCannonShellEntity> {
    public SmallCannonShellRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new SmallCannonShellModel());
        this.addRenderLayer(new SmallCannonShellLayer(this));
    }

    @Override
    public RenderType getRenderType(SmallCannonShellEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, SmallCannonShellEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        if (entity.tickCount > 0) {
            super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
        }
    }

    @Override
    public void defaultRender(PoseStack poseStack, SmallCannonShellEntity animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        if (animatable.tickCount > 0) {
            poseStack.pushPose();

            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, animatable.yRotO, animatable.getYRot()) - 90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90 + Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
            super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);

            poseStack.popPose();
        }
    }
}
