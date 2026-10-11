package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.capability.living.RadiationCapability
import com.atsuishio.superbwarfare.mobeffect.RadiationMobEffect
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.CameraType
import net.minecraft.client.model.EntityModel
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.LivingEntityRenderer
import net.minecraft.client.renderer.entity.layers.RenderLayer
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import kotlin.math.sin

class RadiationGlowRenderer<T : LivingEntity, M : EntityModel<T>>(
    private val livingRenderer: LivingEntityRenderer<T, M>
) : RenderLayer<T, M>(livingRenderer) {

    override fun render(
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        entity: T,
        limbSwing: Float,
        limbSwingAmount: Float,
        partialTick: Float,
        ageInTicks: Float,
        netHeadYaw: Float,
        headPitch: Float
    ) {
        val dose = RadiationCapability.getDosage(entity)
        val symptomThreshold = RadiationMobEffect.getSymptomThreshold()
        if (dose < symptomThreshold || entity.isInvisible) return

        if (entity is Player && entity == localPlayer && mc.options.cameraType == CameraType.FIRST_PERSON) return

        val strength = ((dose - symptomThreshold) / (RadiationMobEffect.getLethalThreshold() - symptomThreshold))
            .coerceIn(0f, 1f)
        val pulse = 0.92f + 0.08f * sin(ageInTicks * 0.08f)
        val alpha = (0.15f + 0.65f * strength) * pulse
        val green = 0.45f + 0.55f * strength
        val glowTexture = livingRenderer.getTextureLocation(entity)

        parentModel.renderToBuffer(
            poseStack,
            buffer.getBuffer(RenderType.entityTranslucentEmissive(glowTexture, false)),
            0xF000F0,
            OverlayTexture.NO_OVERLAY,
            0.05f * (1f - strength),
            green,
            0.08f * (1f - strength),
            alpha
        )
    }
}
