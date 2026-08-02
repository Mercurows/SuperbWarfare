package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance
import com.atsuishio.superbwarfare.entity.vehicle.AnnihilatorEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.ArtilleryEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.util.Mth

class AnnihilatorRenderer(manager: EntityRendererProvider.Context) : BasicArtilleryRenderer(manager) {
    companion object {
        val TEXTURE_GLOW = Mod.loc("textures/bedrock/vehicle/annihilator_glow.png")
        val TEXTURE_POWER = Mod.loc("textures/bedrock/vehicle/annihilator_power.png")
    }

    override fun transformCustomModelPart(
        entity: ArtilleryEntity,
        instance: VehicleModelInstance,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks)

        val laser1 = instance.getBone("laser1")
        val laser2 = instance.getBone("laser2")
        val laser3 = instance.getBone("laser3")

        laser1?.zScale = entity.entityData.get(AnnihilatorEntity.LASER_LEFT_LENGTH) * 10
        laser2?.zScale = entity.entityData.get(AnnihilatorEntity.LASER_MIDDLE_LENGTH) * 10
        laser3?.zScale = entity.entityData.get(AnnihilatorEntity.LASER_RIGHT_LENGTH) * 10

        val energy = entity.chargeProgress

        for (i in 1..5) {
            val greenBoneName = "move_light_on$i"
            val redBoneName = "move_light_off$i"
            val greenBone = instance.getBone(greenBoneName)
            val redBone = instance.getBone(redBoneName)

            if (greenBone != null && redBone != null) {
                greenBone.visible = energy >= (i / 5.0)
                redBone.visible = energy < (i / 5.0)
            }
        }
    }

    override fun customLaserLength(laserBones: List<BoneState>, entity: ArtilleryEntity, partialTicks: Float) {
        for (laser in laserBones) {
            laser.visible = false

            val scale = Mth.lerp(
                partialTicks,
                entity.laserScaleO,
                entity.laserScale
            ).coerceAtMost(1.2f)

            laser.xScale = scale
            laser.yScale = scale
        }
    }

    override fun renderCustomPart(
        entity: ArtilleryEntity,
        instance: VehicleModelInstance,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        super.renderCustomPart(entity, instance, poseStack, entityYaw, partialTicks, buffer, packedLight)

        // power

        val red = 1 - Mth.clamp(2.5f * entity.energy / entity.maxEnergy, 0f, 1f)
        val green = Mth.clamp(2.5f * entity.energy / entity.maxEnergy, 0f, 1f)

        instance.renderToBuffer(
            poseStack,
            buffer,
            RenderType.entityTranslucent(TEXTURE_POWER),
            BedrockModelRenderTypes.polyMeshCutout(TEXTURE_POWER),
            packedLight,
            OverlayTexture.NO_OVERLAY, red, green, 0f, 1f
        )

        instance.renderToBuffer(
            poseStack,
            buffer,
            RenderType.eyes(TEXTURE_POWER),
            BedrockModelRenderTypes.polyMeshCutout(TEXTURE_POWER),
            packedLight,
            OverlayTexture.NO_OVERLAY, red, green, 0f, 1f
        )

        instance.renderToBuffer(
            poseStack,
            buffer,
            RenderType.eyes(TEXTURE_GLOW),
            BedrockModelRenderTypes.polyMeshCutout(TEXTURE_GLOW),
            packedLight,
            OverlayTexture.NO_OVERLAY
        )
    }
}
