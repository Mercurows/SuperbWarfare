package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Mle1934Model;
import com.atsuishio.superbwarfare.entity.vehicle.Mle1934Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class Mle1934Renderer extends VehicleRenderer<Mle1934Entity> {

    public Mle1934Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Mle1934Model());
        this.shadowRadius = 2f;
    }

    @Override
    public void vehicleAxis(Mle1934Entity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateOffsetHeight(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }
}
