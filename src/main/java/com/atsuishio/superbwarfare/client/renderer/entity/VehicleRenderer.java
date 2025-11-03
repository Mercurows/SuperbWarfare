package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public abstract class VehicleRenderer<T extends VehicleEntity & GeoAnimatable> extends GeoEntityRenderer<T> {

    public VehicleRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
    }

    @Override
    public RenderType getRenderType(T vehicle, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(vehicle));
    }

    @Override
    public void preRender(PoseStack poseStack, T vehicle, BakedGeoModel model, @Nullable MultiBufferSource bufferSource, @Nullable VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.preRender(poseStack, vehicle, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void render(T entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
        poseStack.pushPose();
        vehicleAxis(entityIn, poseStack, entityYaw, partialTicks);
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        renderCustomPart(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn);
        poseStack.popPose();
    }

    public void vehicleAxis(T entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateOffsetHeight(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entityIn.prevRoll, entityIn.getRoll())), (float) root.x, (float) root.y, (float) root.z);
    }

    public void renderCustomPart(T entityIn, float entityYaw, float partialTicks, PoseStack poseStack, @NotNull MultiBufferSource bufferIn, int packedLightIn) {
    }

    @Override
    public boolean shouldRender(T vehicle, @NotNull Frustum pCamera, double pCamX, double pCamY, double pCamZ) {
        if (!vehicle.shouldRender(pCamX, pCamY, pCamZ)) {
            return false;
        } else if (vehicle.noCulling) {
            return true;
        } else {
            AABB aabb = vehicle.getBoundingBoxForCulling().inflate(3);
            if (aabb.hasNaN() || aabb.getSize() == 0.0) {
                aabb = new AABB(vehicle.getX() - 5.0, vehicle.getY() - 4.0, vehicle.getZ() - 5.0, vehicle.getX() + 5.0, vehicle.getY() + 4.0, vehicle.getZ() + 5.0);
            }

            return pCamera.isVisible(aabb);
        }
    }
}
