package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.Type63Entity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Type63Renderer(manager: EntityRendererProvider.Context) : BasicVehicleRenderer(manager) {

    override fun transformCustomModelPart(vehicle: VehicleEntity, model: BedrockVehicleModel, poseStack: PoseStack, entityYaw: Float, partialTicks: Float) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)
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
