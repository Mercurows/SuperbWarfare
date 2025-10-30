package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.MortarModel;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class MortarRenderer extends VehicleRenderer<MortarEntity> {

    public MortarRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new MortarModel());
        this.shadowRadius = 0f;
    }

    @Override
    public void vehicleAxis(MortarEntity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateYOffset(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }
}
