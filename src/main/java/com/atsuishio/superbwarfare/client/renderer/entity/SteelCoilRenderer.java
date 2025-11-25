//package com.atsuishio.superbwarfare.client.renderer.entity;
//
//import com.atsuishio.superbwarfare.client.model.entity.SteelCoilModel;
//import com.atsuishio.superbwarfare.entity.projectile.SteelCoilEntity;
//import com.mojang.blaze3d.vertex.PoseStack;
//import com.mojang.blaze3d.vertex.VertexConsumer;
//import com.mojang.math.Axis;
//import net.minecraft.client.renderer.MultiBufferSource;
//import net.minecraft.client.renderer.RenderType;
//import net.minecraft.client.renderer.entity.EntityRendererProvider;
//import net.minecraft.resources.ResourceLocation;
//import software.bernie.geckolib.cache.object.BakedGeoModel;
//import software.bernie.geckolib.renderer.GeoEntityRenderer;
//
//public class SteelCoilRenderer extends GeoEntityRenderer<SteelCoilEntity> {
//
//    public SteelCoilRenderer(EntityRendererProvider.Context renderManager) {
//        super(renderManager, new SteelCoilModel());
//        this.shadowRadius = 0f;
//    }
//
//    @Override
//    public RenderType getRenderType(SteelCoilEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
//        return RenderType.entityTranslucent(getTextureLocation(animatable));
//    }
//
//    @Override
//    public void actuallyRender(PoseStack poseStack, SteelCoilEntity animatable, BakedGeoModel model, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
//        poseStack.pushPose();
//
//        if (animatable.getDeltaMovement().lengthSqr() > 0) {
//            poseStack.mulPose(Axis.YP.rotationDegrees(animatable.yRotO));
////            poseStack.mulPose(Axis.XP.rotationDegrees(Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
//        }
//
//        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
//
//        poseStack.popPose();
//    }
//}
