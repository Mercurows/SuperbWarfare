package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.base.AutoAimableEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.options
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.CameraType
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth

class Hpj11Renderer(manager: EntityRendererProvider.Context) : BasicAutoAimableRenderer(manager) {
    override fun hideForTurretControllerWhileZooming(): Boolean {
        return true
    }

    override fun transformCustomModelPart(
        vehicle: AutoAimableEntity,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val radar2 = model.getBone("radar2")

        radar2.visible = !(vehicle.getNthEntity(vehicle.turretControllerIndex) === localPlayer && (options.cameraType == CameraType.FIRST_PERSON || ClientEventHandler.zoomVehicle))

        val rdr = model.getBone("rdr")
        val rdr2 = model.getBone("rdr2")

        val rot = Mth.clamp(-turretXRot, vehicle.turretMinPitch, vehicle.turretMaxPitch) * Mth.DEG_TO_RAD

        rdr.rotation.rotationX(rot)
        rdr2.rotation.rotationX(rot)
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

        val heat = Mth.clamp(vehicle.getWeaponHeat(0).toFloat(), 0f, 100f)

        if (heat > 0) {
            model.renderToBuffer(
                poseStack,
                buffer.getBuffer(RenderType.eyes(HEAT)),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                heat / 100,
                heat / 100,
                heat / 100,
                1f
            )
        }
    }

    companion object {
        val HEAT = loc("textures/bedrock/vehicle/hpj_11_heat.png")
    }
}
