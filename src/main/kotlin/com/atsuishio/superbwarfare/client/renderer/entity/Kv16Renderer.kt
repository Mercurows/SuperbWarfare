package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.Kv16Entity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.util.Mth

class Kv16Renderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : Kv16Entity, T : BasicGeoVehicleEntity {
    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val root = model.getBone("root")
        root.visible = !(hideForTurretControllerWhileZooming && vehicle.getWeaponIndex(0) == 1)

        val propeller = model.getBone("propeller")

        propeller.rotation.rotateZ(Mth.lerp(partialTicks, vehicle.propellerRotO, vehicle.propellerRot))
    }
}
