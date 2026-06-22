package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation

class LaserTowerRenderer(manager: EntityRendererProvider.Context) : BasicAutoAimableRenderer(manager){
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun renderCustomPart(
        vehicle: AutoAimableEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        super.renderCustomPart(vehicle, model, poseStack, entityYaw, partialTicks, buffer, packedLight)

        if (vehicle.energy > 0 && vehicle.active) {
            model.renderToBuffer(
                poseStack,
                buffer,
                RenderType.energySwirl(LASER, 1f, 1f),
                BedrockModelRenderTypes.polyMeshCutout(LASER),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }
    }

    override fun getEmissiveTextureLocation(poseStack: PoseStack, entity: AutoAimableEntity): ResourceLocation? {
        return if (entity.energy > 0 && entity.active) {
            super.getEmissiveTextureLocation(poseStack, entity)
        } else {
            null
        }
    }

    companion object {
        val LASER = Mod.loc("textures/bedrock/vehicle/laser_tower_laser.png")
    }
}
