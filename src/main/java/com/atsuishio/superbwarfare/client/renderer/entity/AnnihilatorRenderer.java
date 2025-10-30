package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.*;
import com.atsuishio.superbwarfare.client.model.entity.AnnihilatorModel;
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class AnnihilatorRenderer extends VehicleRenderer<AnnihilatorEntity> {

    public AnnihilatorRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AnnihilatorModel());
        this.addRenderLayer(new AnnihilatorLayer(this));
        this.addRenderLayer(new AnnihilatorGlowLayer(this));
        this.addRenderLayer(new AnnihilatorPowerLayer(this));
        this.addRenderLayer(new AnnihilatorPowerLightLayer(this));
        this.addRenderLayer(new AnnihilatorLedLayer(this));
        this.addRenderLayer(new AnnihilatorLedLightLayer(this));
    }

    @Override
    public void vehicleAxis(AnnihilatorEntity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateYOffset(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }
}
