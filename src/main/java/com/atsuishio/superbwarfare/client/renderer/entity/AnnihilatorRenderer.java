package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.*;
import com.atsuishio.superbwarfare.client.model.entity.AnnihilatorModel;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
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

public class AnnihilatorRenderer extends GeoEntityRenderer<AnnihilatorEntity> {

    public AnnihilatorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AnnihilatorModel());
        this.addRenderLayer(new AnnihilatorLayer(this));
        this.addRenderLayer(new AnnihilatorGlowLayer(this));
        this.addRenderLayer(new AnnihilatorPowerLayer(this));
        this.addRenderLayer(new AnnihilatorPowerLightLayer(this));
        this.addRenderLayer(new AnnihilatorLedLayer(this));
        this.addRenderLayer(new AnnihilatorLedLightLayer(this));
    }

    @Override
    public RenderType getRenderType(AnnihilatorEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, AnnihilatorEntity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    @Override
    public void render(AnnihilatorEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot())));
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    protected float getDeathMaxRotation(AnnihilatorEntity entityLivingBaseIn) {
        return 0.0F;
    }
}
