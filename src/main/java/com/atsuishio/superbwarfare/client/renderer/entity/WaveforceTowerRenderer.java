package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.WaveforceTowerGlowLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.WaveforceTowerLaserLayer;
import com.atsuishio.superbwarfare.client.model.entity.WaveforceTowerModel;
import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class WaveforceTowerRenderer extends VehicleRenderer<WaveforceTowerEntity> {

    public WaveforceTowerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new WaveforceTowerModel());
        this.addRenderLayer(new WaveforceTowerGlowLayer(this));
        this.addRenderLayer(new WaveforceTowerLaserLayer(this));
    }

    @Override
    public void vehicleAxis(WaveforceTowerEntity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateYOffset(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }
}
