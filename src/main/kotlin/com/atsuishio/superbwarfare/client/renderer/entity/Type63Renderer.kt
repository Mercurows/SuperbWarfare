package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Type63Renderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : Type63Entity, T : BasicGeoVehicleEntity {

    override fun transformCustomModelPart(vehicle: T, model: BedrockVehicleModel, poseStack: PoseStack, entityYaw: Float, partialTicks: Float, buffer: MultiBufferSource, packedLight: Int) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks, buffer, packedLight)
        val shouLunX = model.getBone("shoulunx")
        val shouLunY = model.getBone("shouluny")

        shouLunX.rotation.rotationX(-turretXRot * 3)
        shouLunY.rotation.rotationZ(turretYRot * 6)

        model.shell.forEachIndexed { index, bone ->
            val items = vehicle.entityData.get(Type63Entity.LOADED_AMMO)
            bone.visible = items[index] != -1
        }

    }
}
