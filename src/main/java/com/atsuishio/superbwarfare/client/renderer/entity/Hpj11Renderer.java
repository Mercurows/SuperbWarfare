package com.atsuishio.superbwarfare.client.renderer.entity;

import com.atsuishio.superbwarfare.client.layer.vehicle.Hpj11HeatLayer;
import com.atsuishio.superbwarfare.client.layer.vehicle.Hpj11Layer;
import com.atsuishio.superbwarfare.client.model.entity.Hpj11Model;
import com.atsuishio.superbwarfare.entity.vehicle.Hpj11Entity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.phys.Vec3;

public class Hpj11Renderer extends VehicleRenderer<Hpj11Entity> {

    public Hpj11Renderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new Hpj11Model());
        this.shadowRadius = 1.5f;
        this.addRenderLayer(new Hpj11Layer(this));
        this.addRenderLayer(new Hpj11HeatLayer(this));
    }

    @Override
    public void vehicleAxis(Hpj11Entity entityIn, PoseStack poseStack, float entityYaw, float partialTicks) {
        Vec3 root = new Vec3(0, entityIn.rotateOffsetHeight(), 0);
        poseStack.rotateAround(Axis.YP.rotationDegrees(-entityYaw), (float) root.x, (float) root.y, (float) root.z);
    }
}
