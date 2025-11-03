package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.TowModel;
import com.atsuishio.superbwarfare.entity.vehicle.TowEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class TowRenderer extends VehicleRenderer<TowEntity> {

    public TowRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new TowModel());
    }

    @Override
    public void vehicleAxis(TowEntity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateOffsetHeight(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }
}
