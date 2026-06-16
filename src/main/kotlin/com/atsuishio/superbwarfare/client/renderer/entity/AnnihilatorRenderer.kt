package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth

class AnnihilatorRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : AnnihilatorEntity, T : BasicGeoVehicleEntity {
    companion object {
        val TEXTURE_GLOW = Mod.loc("textures/bedrock/vehicle/annihilator_glow.png")
        val TEXTURE_LASER = Mod.loc("textures/bedrock/vehicle/annihilator_laser.png")
        val TEXTURE_POWER = Mod.loc("textures/bedrock/vehicle/annihilator_power.png")
    }

    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)

        val laser1 = model.getBone("laser1")
        val laser2 = model.getBone("laser2")
        val laser3 = model.getBone("laser3")

        laser1.zScale = vehicle.entityData.get(AnnihilatorEntity.LASER_LEFT_LENGTH)
        laser2.zScale = vehicle.entityData.get(AnnihilatorEntity.LASER_MIDDLE_LENGTH)
        laser3.zScale = vehicle.entityData.get(AnnihilatorEntity.LASER_RIGHT_LENGTH)

        val energy = vehicle.chargeProgress

        for (i in 1..5) {
            val greenBoneName = "light_on$i"
            val redBoneName = "light_off$i"
            val greenBone = model.getBone(greenBoneName)
            val redBone = model.getBone(redBoneName)

            if (greenBone != null && redBone != null) {
                greenBone.visible = energy >= (i / 5.0)
                redBone.visible = energy < (i / 5.0)
            }
        }
    }

    override fun renderCustomPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        super.renderCustomPart(vehicle, model, poseStack, entityYaw, partialTicks, buffer, packedLight)

        // power

        val red = 1 - Mth.clamp(2.5f * vehicle.energy / vehicle.maxEnergy, 0f, 1f)
        val green = Mth.clamp(2.5f * vehicle.energy / vehicle.maxEnergy, 0f, 1f)

        model.renderToBuffer(
            poseStack,
            buffer,
            RenderType.entityTranslucent(TEXTURE_POWER),
            BedrockModelRenderTypes.polyMeshCutout(TEXTURE_POWER),
            packedLight,
            OverlayTexture.NO_OVERLAY, red, green, 0f, 1f
        )

        model.renderToBuffer(
            poseStack,
            buffer,
            ModRenderTypes.LASER.apply(TEXTURE_POWER),
            BedrockModelRenderTypes.polyMeshCutout(TEXTURE_POWER),
            packedLight,
            OverlayTexture.NO_OVERLAY, red, green, 0f, 1f
        )

        if (vehicle.chargeProgress < 1) {
            // glow

            model.renderToBuffer(
                poseStack,
                buffer,
                ModRenderTypes.LASER.apply(TEXTURE_GLOW),
                BedrockModelRenderTypes.polyMeshCutout(TEXTURE_GLOW),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )

            // laser

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
