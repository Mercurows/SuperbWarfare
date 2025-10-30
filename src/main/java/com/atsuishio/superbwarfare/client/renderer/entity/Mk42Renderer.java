package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Mk42Model;
import com.atsuishio.superbwarfare.entity.vehicle.Mk42Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class Mk42Renderer extends VehicleRenderer<Mk42Entity> {

    public Mk42Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Mk42Model());
        this.shadowRadius = 2f;
    }

    @Override
    public void vehicleAxis(Mk42Entity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateYOffset(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }
}
