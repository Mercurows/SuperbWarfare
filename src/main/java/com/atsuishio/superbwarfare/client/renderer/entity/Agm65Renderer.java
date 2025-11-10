package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.projectile.Agm65Layer;
import com.atsuishio.superbwarfare.client.model.entity.Agm65Model;
import com.atsuishio.superbwarfare.entity.projectile.Agm65Entity;
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

public class Agm65Renderer extends GeoEntityRenderer<Agm65Entity> {
    public Agm65Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Agm65Model());
        this.addRenderLayer(new Agm65Layer(this));
    }

    @Override
    public RenderType getRenderType(Agm65Entity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void defaultRender(PoseStack poseStack, Agm65Entity entityIn, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTicks, int packedLight) {
        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.yRotO, entityIn.getYRot())));
        poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())));

        super.defaultRender(poseStack, entityIn, bufferSource, renderType, buffer, yaw, partialTicks, packedLight);
     
        poseStack.popPose();
    }
}
