package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.Lav25Entity
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource
import com.atsuishio.superbwarfare.script.VehicleScriptManager
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.entity.EntityRendererProvider

class Lav25Renderer(manager: EntityRendererProvider.Context) : SbmVehicleRenderer<Lav25Entity>(manager) {
    // TODO 测试用
    override fun transformCustomModelPart(
        vehicle: Lav25Entity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        val func = VehicleResource.getDefault(vehicle).getScript() ?: return
        VehicleScriptManager.invokeTransform(func, vehicle, model, poseStack, entityYaw, partialTicks, this)
    }
}
