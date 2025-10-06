package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.TruckModel;
import com.atsuishio.superbwarfare.entity.vehicle.TruckEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class TruckRenderer extends GeoEntityRenderer<TruckEntity> {

    public TruckRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TruckModel());
//        this.addRenderLayer(new TruckLayer(this));
    }

    @Override
    public RenderType getRenderType(TruckEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void render(TruckEntity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        Vec3 root = new Vec3(0, entityIn.rotateYOffset(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entityIn.prevRoll, entityIn.getRoll())), (float) root.x, (float) root.y, (float) root.z);
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, TruckEntity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();
        if (name.equals("control")) {
            bone.setRotY(12 * Mth.lerp(partialTick, animatable.rudderRotO, animatable.getRudderRot()));
        }
        if (name.equals("wheel0")) {
            bone.setRotY(1.3f * Mth.lerp(partialTick, animatable.rudderRotO, animatable.getRudderRot()));
            bone.setRotX(Mth.lerp(partialTick, animatable.leftWheelRotO, animatable.getLeftWheelRot()));
        }
        if (name.equals("wheel1")) {
            bone.setRotY(1.3f * Mth.lerp(partialTick, animatable.rudderRotO, animatable.getRudderRot()));
            bone.setRotX(Mth.lerp(partialTick, animatable.rightWheelRotO, animatable.getRightWheelRot()));
        }
        if (name.equals("wheel2")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.rightWheelRotO, animatable.getRightWheelRot()));
        }
        if (name.equals("wheel3")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.leftWheelRotO, animatable.getLeftWheelRot()));
        }
        if (name.equals("wheel4")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.rightWheelRotO, animatable.getRightWheelRot()));
        }
        if (name.equals("wheel5")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.leftWheelRotO, animatable.getLeftWheelRot()));
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
}
