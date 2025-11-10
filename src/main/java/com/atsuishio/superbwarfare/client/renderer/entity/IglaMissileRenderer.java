package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.projectile.IglaMissileLayer;
import com.atsuishio.superbwarfare.client.model.entity.IglaMissileModel;
import com.atsuishio.superbwarfare.entity.projectile.IglaMissileEntity;
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

public class IglaMissileRenderer extends GeoEntityRenderer<IglaMissileEntity> {
    public IglaMissileRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new IglaMissileModel());
        this.addRenderLayer(new IglaMissileLayer(this));
    }

    @Override
    public RenderType getRenderType(IglaMissileEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void defaultRender(PoseStack poseStack, IglaMissileEntity entityIn, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTicks, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));

        super.defaultRender(poseStack, entityIn, bufferSource, renderType, buffer, yaw, partialTicks, packedLight);
     
        poseStack.popPose();
    }
}
