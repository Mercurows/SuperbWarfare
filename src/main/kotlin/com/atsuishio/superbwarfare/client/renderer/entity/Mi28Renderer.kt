package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.Mi28Entity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class Mi28Renderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : Mi28Entity, T : BasicGeoVehicleEntity {
    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {

        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)
        val propeller = model.getBone("propeller")
        val tailPropeller = model.getBone("tailPropeller")

        propeller.rotation.rotateY(-Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
        tailPropeller.rotation.rotateX(6 * Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
    }
}
