package com.atsuishio.superbwarfare.client.renderer.entity;

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
        if (entity.tickCount > 1) {
            float scale = 1f;
            this.scaleHeight = scale;
            this.scaleWidth = scale;
            super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
        }
    }

    @Override
    public void render(ProjectileEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
        if (entityIn.tickCount > 1) {
            poseStack.pushPose();
            poseStack.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot()) - 90));
            poseStack.mulPose(Axis.ZP.rotationDegrees(90 + Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));
            super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
            poseStack.popPose();
        }
    }

    @Override
    protected float getDeathMaxRotation(ProjectileEntity entityLivingBaseIn) {
        return 0.0F;
    }
}
