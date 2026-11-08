package com.atsuishio.superbwarfare.client.particle

import com.atsuishio.superbwarfare.client.renderer.ModParticleRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.data.ParticleEffectDefinition
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleEmitterInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.runtime.ParticleInstance
import com.github.mcmodderanchor.simplebedrockmodel.v1.particle.world.SnowStormParticle
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth

class MuzzleSmokeParticle(
    level: ClientLevel,
    private val data: ParticleInstance,
    definition: ParticleEffectDefinition,
    emitter: ParticleEmitterInstance,
) : SnowStormParticle(level, data, definition, emitter) {
    private val smokeRenderType = renderTypes.getOrPut(definition.description.texture) {
        ModParticleRenderTypes.softTranslucent(definition.description.texture)
    }
    private var previousWidth = data.width
    private var previousHeight = data.height
    private var previousAlpha = alpha
    private var previousU0 = data.u0
    private var previousU1 = data.u1
    private var previousV0 = data.v0
    private var previousV1 = data.v1

    override fun getRenderType(): ParticleRenderType = smokeRenderType

    override fun tick() {
        previousWidth = data.width
        previousHeight = data.height
        previousAlpha = alpha
        previousU0 = data.u0
        previousU1 = data.u1
        previousV0 = data.v0
        previousV1 = data.v1
        super.tick()
    }

    override fun render(buffer: VertexConsumer, camera: Camera, partialTicks: Float) {
        val width = data.width
        val height = data.height
        val opacity = alpha
        val u0 = data.u0
        val u1 = data.u1
        val v0 = data.v0
        val v1 = data.v1
        val t = partialTicks.coerceIn(0f, 1f)
        val smoothAlpha = Mth.lerp(t, previousAlpha, opacity)
        try {
            data.width = Mth.lerp(t, previousWidth, width)
            data.height = Mth.lerp(t, previousHeight, height)
            // Blend adjacent flipbook frames instead of snapping the entire puff every few ticks.
            if (previousU0 != u0 || previousV0 != v0) {
                data.u0 = previousU0
                data.u1 = previousU1
                data.v0 = previousV0
                data.v1 = previousV1
                alpha = smoothAlpha * (1f - t)
                super.render(buffer, camera, partialTicks)
                data.u0 = u0
                data.u1 = u1
                data.v0 = v0
                data.v1 = v1
                alpha = smoothAlpha * t
            } else {
                alpha = smoothAlpha
            }
            super.render(buffer, camera, partialTicks)
        } finally {
            data.width = width
            data.height = height
            data.u0 = u0
            data.u1 = u1
            data.v0 = v0
            data.v1 = v1
            alpha = opacity
        }
    }

    companion object {
        private val renderTypes = mutableMapOf<ResourceLocation, ParticleRenderType>()
    }
}
