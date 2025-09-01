package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.MedicalKitModel;
import com.atsuishio.superbwarfare.entity.MedicalKitEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MedicalKitEntityRenderer extends GeoEntityRenderer<MedicalKitEntity> {

    public MedicalKitEntityRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MedicalKitModel());
        this.shadowRadius = 0f;
    }

    @Override
    public RenderType getRenderType(MedicalKitEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void render(MedicalKitEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        if (entityIn.getDeltaMovement().lengthSqr() > 0) {
            poseStack.mulPose(Axis.YP.rotationDegrees(-entityYaw));
            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot()) + 90));
        }
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }
}
