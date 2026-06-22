package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

open class WheelChairRenderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {
    override fun transformCustomModelPart(
        vehicle: VehicleEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        val wRb = model.getBone("w_rb")
        val wLb = model.getBone("w_lb")
        val wRr = model.getBone("w_rr")
        val wLr = model.getBone("w_lr")

        wRb.rotation.rotationX(Mth.lerp(partialTicks, vehicle.rightWheelRotO, vehicle.rightWheelRot))
        wLb.rotation.rotationX(Mth.lerp(partialTicks, vehicle.leftWheelRotO, vehicle.leftWheelRot))
        wRr.rotation.rotationX(4 * Mth.lerp(partialTicks, vehicle.rightWheelRotO, vehicle.rightWheelRot))
        wLr.rotation.rotationX(4 * Mth.lerp(partialTicks, vehicle.leftWheelRotO, vehicle.leftWheelRot))
    }
}