package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class Ah6Renderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {
    override fun transformCustomModelPart(
        vehicle: VehicleEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)
        val propeller = model.getBone("propeller")
        val tailPropeller = model.getBone("tailPropeller")

        propeller.rotation.rotationY(Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
        tailPropeller.rotation.rotationX(-6 * Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
    }
}
