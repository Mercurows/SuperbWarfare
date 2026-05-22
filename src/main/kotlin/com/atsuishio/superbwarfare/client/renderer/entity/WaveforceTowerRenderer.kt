package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.WaveforceTowerEntity
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import java.util.regex.Pattern

class WaveforceTowerRenderer<T>(manager: EntityRendererProvider.Context) :
    SbmVehicleRenderer<T>(manager) where T : WaveforceTowerEntity, T : BasicGeoVehicleEntity {

    var energy0: Float = 0f
    private val LIGHT_PATTERN: Pattern = Pattern.compile("^light_(?<type>on|off)(?<id>\\d+)")

    override fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        super.transformCustomModelPart(vehicle, model, poseStack, entityYaw, partialTicks)


        val glow = model.getBone("glow")

        val scale = Mth.lerp(
            partialTicks,
            vehicle.laserScaleO,
            vehicle.laserScale,
        ).coerceAtMost(1.2f)

        glow.xScale = scale
        glow.yScale = scale
        glow.zScale = scale

        val glow2 = model.getBone("glow2")

        glow2.z = -16f * vehicle.laserLength - 2.5f

        glow2.xScale = scale
        glow2.yScale = scale
        glow2.zScale = scale

        val charge = model.getBone("charge")

        val energy = vehicle.chargeProgress
        val energyRate0 = energy0
        charge.zScale = Mth.lerp(partialTicks, energyRate0, energy)
        energy0 = energy

        // TODO 实现充能进度灯

//        model.waveForceLight.forEachIndexed { index, bone ->
//            val isOn = matcher.group("type") == "on"
//            val energy = vehicle.chargeProgress
//            val shouldTurnOn = energy >= index / 7f
//
//            bone.visible = shouldTurnOn == isOn
//        }
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

        if (vehicle.energy > 0 && vehicle.active) {
            model.renderToBuffer(
                poseStack,
                buffer,
                ModRenderTypes.LASER.apply(getGlowTextureLocation()),
                BedrockModelRenderTypes.polyMeshCutout(getLaserTextureLocation()),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }

        if (vehicle.laserScale > 0) {
            model.renderToBuffer(
                poseStack,
                buffer,
                RenderType.energySwirl(getLaserTextureLocation(), 1f, 1f),
                BedrockModelRenderTypes.polyMeshCutout(getLaserTextureLocation()),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }
    }

    fun getGlowTextureLocation(): ResourceLocation {
        return Mod.loc("textures/bedrock/vehicle/waveforce_tower_glow.png")
    }

    fun getLaserTextureLocation(): ResourceLocation {
        return Mod.loc("textures/bedrock/vehicle/waveforce_tower_laser.png")
    }
}
