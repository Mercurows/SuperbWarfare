package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.MortarModel
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.world.phys.Vec3

class MortarRenderer(renderManager: EntityRendererProvider.Context) :
    VehicleRenderer<MortarEntity>(renderManager, MortarModel()) {

    init {
        this.shadowRadius = 0f
    }

    override fun vehicleAxis(
        entityIn: MortarEntity,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        val root = Vec3(0.0, entityIn.rotateOffsetHeight, 0.0)
        poseStack.rotateAround(
            Axis.YP.rotationDegrees(-entityYaw),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
    }
}
