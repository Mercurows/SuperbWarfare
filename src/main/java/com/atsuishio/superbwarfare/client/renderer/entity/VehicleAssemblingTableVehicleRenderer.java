package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.VehicleAssemblingTableVehicleModel;
import com.atsuishio.superbwarfare.entity.vehicle.VehicleAssemblingTableVehicleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class VehicleAssemblingTableVehicleRenderer extends GeoEntityRenderer<VehicleAssemblingTableVehicleEntity> {

    public VehicleAssemblingTableVehicleRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new VehicleAssemblingTableVehicleModel());
        this.shadowRadius = 0.5f;
    }

    @Override
    public RenderType getRenderType(VehicleAssemblingTableVehicleEntity animatable, ResourceLocation texture, MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(getTextureLocation(animatable));
    }

    @Override
    public void defaultRender(PoseStack poseStack, VehicleAssemblingTableVehicleEntity animatable, MultiBufferSource bufferSource, @Nullable RenderType renderType, @Nullable VertexConsumer buffer, float yaw, float partialTick, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(-0.5, -0.01, 0.5);

        Vec3 root = new Vec3(0.5, 0.5, -0.5);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-yaw), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.XP.rotationDegrees(Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())), (float) root.x, (float) root.y, (float) root.z);
        poseStack.rotateAround(Axis.ZP.rotationDegrees(Mth.lerp(partialTick, animatable.prevRoll, animatable.getRoll())), (float) root.x, (float) root.y, (float) root.z);
        super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight);
        poseStack.popPose();
    }
}
