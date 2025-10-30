package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.LaserTowerLaserLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.LaserTowerPowerLayer;
import com.atsuishio.superbwarfare.client.model.entity.LaserTowerModel;
import com.atsuishio.superbwarfare.entity.vehicle.LaserTowerEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class LaserTowerRenderer extends VehicleRenderer<LaserTowerEntity> {

    public LaserTowerRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new LaserTowerModel());
        this.addRenderLayer(new LaserTowerPowerLayer(this));
        this.addRenderLayer(new LaserTowerLaserLayer(this));
    }

    @Override
    public void vehicleAxis(LaserTowerEntity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateYOffset(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }

}
