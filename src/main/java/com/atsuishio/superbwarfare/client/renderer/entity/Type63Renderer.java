package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.model.entity.Type63Model;
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;


public class Type63Renderer extends VehicleRenderer<Type63Entity> {

    public Type63Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Type63Model());
        this.shadowRadius = 0.8f;
    }

    @Override
    public void vehicleAxis(Type63Entity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateOffsetHeight(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }
}
