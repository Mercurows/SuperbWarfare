package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource
import com.atsuishio.superbwarfare.script.VehicleScriptManager
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture

class WaveforceTowerRenderer(manager: EntityRendererProvider.Context) : BasicAutoAimableRenderer(manager){
    companion object {
        val TEXTURE_LASER = loc("textures/bedrock/vehicle/waveforce_tower_laser.png")
    }

    @Suppress("unused")
    var energy0: Float = 0f

    // TODO 测试用
    override fun transformCustomModelPart(
        vehicle: AutoAimableEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        val func = VehicleResource.getDefault(vehicle).getScript() ?: return
        VehicleScriptManager.invokeTransform(func, vehicle, model, poseStack, entityYaw, partialTicks, this)
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)
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
            val emissive = this.getEmissiveTextureLocation(poseStack, vehicle)
            model.renderToBuffer(
                poseStack,
                buffer,
                ModRenderTypes.LASER.apply(emissive),
                BedrockModelRenderTypes.polyMeshCutout(emissive),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }

        if (vehicle.laserScale > 0) {
            model.renderToBuffer(
                poseStack,
                buffer,
                RenderType.energySwirl(TEXTURE_LASER, 1f, 1f),
                BedrockModelRenderTypes.polyMeshCutout(TEXTURE_LASER),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }
    }
}
