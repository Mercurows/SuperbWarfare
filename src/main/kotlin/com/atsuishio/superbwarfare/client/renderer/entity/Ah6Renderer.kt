package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class Ah6Renderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : VehicleEntity, T : BasicGeoVehicleEntity {
    override fun transformCustomModelPart(vehicle: T, model: BedrockVehicleModel, poseStack: PoseStack, entityYaw: Float, partialTicks: Float, buffer: MultiBufferSource, packedLight: Int) {

        // TODO 实现lod功能
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks, buffer, packedLight)
        val propeller = model.getBone("propeller")
        val tailPropeller = model.getBone("tailPropeller")

        propeller.rotation.rotationY(Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
        tailPropeller.rotation.rotationX(-6 * Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))

    }
}
