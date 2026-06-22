package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

class MortarRenderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {

    override fun transformCustomModelPart(
        vehicle: VehicleEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        val paoguan = model.getBone("paoguan")
        val monitor = model.getBone("monitor")
        val jiaojia = model.getBone("jiaojia")
        val headPitch = -Mth.lerp(partialTicks, vehicle.xRotO, vehicle.xRot)

        paoguan.rotation.rotationX(headPitch * Mth.DEG_TO_RAD)
        jiaojia.rotation.rotationX(-2 * ((headPitch - (10 - headPitch * 0.1f)) * Mth.DEG_TO_RAD))
        monitor.visible = vehicle.entityData.get(MortarEntity.INTELLIGENT)

    }

    override fun rotateVehicleAxis(entityIn: VehicleEntity, poseStack: PoseStack, entityYaw: Float, partialTicks: Float) {
        val root = Vec3(0.0, entityIn.rotateOffsetHeight, 0.0)
        poseStack.rotateAround(
            Axis.YP.rotationDegrees(-entityYaw + 180),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
    }
}
