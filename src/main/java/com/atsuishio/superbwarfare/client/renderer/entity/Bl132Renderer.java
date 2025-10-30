package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Bl132Model;
import com.atsuishio.superbwarfare.entity.vehicle.Bl132Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class Bl132Renderer extends VehicleRenderer<Bl132Entity> {

    public Bl132Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Bl132Model());
        this.shadowRadius = 2f;
    }

    @Override
    public void vehicleAxis(Bl132Entity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateYOffset(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }

}
