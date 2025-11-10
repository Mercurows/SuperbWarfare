package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.ClientRenderHandler;
import com.atsuishio.superbwarfare.client.layer.projectile.ProjectileEntityInsideLayer;
import com.atsuishio.superbwarfare.client.layer.projectile.ProjectileEntityLayer;
import com.atsuishio.superbwarfare.client.model.entity.ProjectileEntityModel;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
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

public class ProjectileEntityRenderer extends GeoEntityRenderer<ProjectileEntity> {

    public ProjectileEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new ProjectileEntityModel());
        this.shadowRadius = 0f;
        this.addRenderLayer(new ProjectileEntityLayer(this));
        this.addRenderLayer(new ProjectileEntityInsideLayer(this));
    }

    @Override
    public RenderType getRenderType(ProjectileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.energySwirl(getTextureLocation(animatable), 1, 1);
    }

    @Override
    public void preRender(PoseStack poseStack, ProjectileEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        if (entity.tickCount > 1 && !entity.isInWater()) {
            super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
        }
    }

    @Override
    public void defaultRender(PoseStack poseStack, ProjectileEntity entityIn, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        if (entityIn.tickCount > 1 && !entityIn.isInWater()) {
            poseStack.pushPose();

            ClientRenderHandler.transformVirtualRenderPosition(poseStack, entityIn, partialTick);

            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTick, entityIn.yRotO, entityIn.getYRot()) - 90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90 + Mth.lerp(partialTick, entityIn.xRotO, entityIn.getXRot())));

            super.defaultRender(poseStack, entityIn, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
            poseStack.popPose();
        }
    }

    @Override
    protected float getDeathMaxRotation(ProjectileEntity entityLivingBaseIn) {
        return 0;
    }
}
