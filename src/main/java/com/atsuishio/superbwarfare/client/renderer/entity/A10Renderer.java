package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.A10Model;
import com.atsuishio.superbwarfare.entity.vehicle.A10Entity;
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
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

import static com.atsuishio.superbwarfare.entity.vehicle.A10Entity.LOADED_BOMB;
import static com.atsuishio.superbwarfare.entity.vehicle.A10Entity.LOADED_MISSILE;
import static com.atsuishio.superbwarfare.entity.vehicle.base.MobileVehicleEntity.GEAR_ROT;

public class A10Renderer extends GeoEntityRenderer<A10Entity> {

    public A10Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new A10Model());
        this.shadowRadius = 0.5f;
    }

    @Override
    public RenderType getRenderType(A10Entity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void preRender(PoseStack poseStack, A10Entity entity, BakedGeoModel model, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        float scale = 1f;
        this.scaleHeight = scale;
        this.scaleWidth = scale;
        super.preRender(poseStack, entity, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }

    @Override
    public void render(A10Entity entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        Vec3 root = new Vec3(0, 2.375, 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entityIn.prevRoll, entityIn.getRoll())), (float) root.x, (float) root.y, (float) root.z);
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    @Override
    public void renderRecursively(PoseStack poseStack, A10Entity animatable, GeoBone bone, RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int color) {
        String name = bone.getName();
        if (name.equals("wingLR")) {
            bone.setRotX(1.5f * Mth.lerp(partialTick, animatable.flap1LRotO, animatable.getFlap1LRot()) * Mth.DEG_TO_RAD);
        }
        if (name.equals("wingRR")) {
            bone.setRotX(1.5f * Mth.lerp(partialTick, animatable.flap1RRotO, animatable.getFlap1RRot()) * Mth.DEG_TO_RAD);
        }
        if (name.equals("wingLR2")) {
            bone.setRotX(1.5f * Mth.lerp(partialTick, animatable.flap1L2RotO, animatable.getFlap1L2Rot()) * Mth.DEG_TO_RAD);
        }
        if (name.equals("wingRR2")) {
            bone.setRotX(1.5f * Mth.lerp(partialTick, animatable.flap1R2RotO, animatable.getFlap1R2Rot()) * Mth.DEG_TO_RAD);
        }
        if (name.equals("wingLB")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.flap2LRotO, animatable.getFlap2LRot()) * Mth.DEG_TO_RAD);
        }
        if (name.equals("wingRB")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.flap2RRotO, animatable.getFlap2RRot()) * Mth.DEG_TO_RAD);
        }
        if (name.equals("weiyiL") || name.equals("weiyiR")) {
            bone.setRotY(Mth.clamp(Mth.lerp(partialTick, animatable.flap3RotO, animatable.getFlap3Rot()), -20f, 20f) * Mth.DEG_TO_RAD);
        }
        if (name.equals("gear") || name.equals("gear2") || name.equals("gear3")) {
            bone.setRotX(Mth.lerp(partialTick, animatable.gearRotO, animatable.getEntityData().get(GEAR_ROT)) * Mth.DEG_TO_RAD);
        }
        if (name.equals("qianzhou") || name.equals("qianzhou2")) {
            bone.setRotZ(Mth.lerp(partialTick, animatable.propellerRotO, animatable.getPropellerRot()));
        }
        if (name.equals("bomb1")) {
            bone.setHidden(animatable.getEntityData().get(LOADED_BOMB) < 3);
        }
        if (name.equals("bomb2")) {
            bone.setHidden(animatable.getEntityData().get(LOADED_BOMB) < 2);
        }
        if (name.equals("bomb3")) {
            bone.setHidden(animatable.getEntityData().get(LOADED_BOMB) < 1);
        }
        if (name.equals("missile1")) {
            bone.setHidden(animatable.getEntityData().get(LOADED_MISSILE) < 4);
        }
        if (name.equals("missile2")) {
            bone.setHidden(animatable.getEntityData().get(LOADED_MISSILE) < 3);
        }
        if (name.equals("missile4")) {
            bone.setHidden(animatable.getEntityData().get(LOADED_MISSILE) < 2);
        }
        if (name.equals("missile3")) {
            bone.setHidden(animatable.getEntityData().get(LOADED_MISSILE) < 1);
        }
        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, color);
    }
}
