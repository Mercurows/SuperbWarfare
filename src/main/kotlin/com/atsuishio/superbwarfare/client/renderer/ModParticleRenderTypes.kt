package com.atsuishio.superbwarfare.client.renderer

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureManager
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.event.RegisterShadersEvent

@OnlyIn(Dist.CLIENT)
object ModParticleRenderTypes {

    private var softParticleShader: ShaderInstance? = null

    /**
     * Called from Mod.kt via RegisterShadersEvent on MOD_BUS.
     * Registers a custom particle shader that does NOT use alpha cutoff (discard),
     * enabling smooth soft-edged transparency unlike the vanilla particle shader.
     */
    fun onRegisterShaders(event: RegisterShadersEvent) {
        val resourceProvider = event.resourceProvider
        event.registerShader(
            ShaderInstance(
                resourceProvider,
                loc("rendertype_particle_soft"),
                DefaultVertexFormat.PARTICLE
            )
        ) { shader -> softParticleShader = shader }
    }

    /**
     * A custom [ParticleRenderType] that uses a shader without the
     * `if (color.a < 0.1) discard;` line found in the vanilla particle shader.
     *
     * This prevents hard transparency edges on high-resolution soft-edged
     * semi-transparent particle textures (e.g. flares, smoke, explosions).
     */
    val PARTICLE_SHEET_SOFT_TRANSLUCENT: ParticleRenderType = object : ParticleRenderType {
        override fun begin(builder: Tesselator, textureManager: TextureManager): BufferBuilder {
            // Override the shader set by ParticleEngine with our no-discard variant
            val shader = softParticleShader
            if (shader != null) {
                RenderSystem.setShader { shader }
            }
            // Use standard alpha blending but disable depth writing.
            // depthMask(false) is essential here because our shader no longer
            // discards low-alpha pixels — without it, semi-transparent edge pixels
            // would write depth and occlude particles behind them.
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.depthMask(false)
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES)
            return builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE)
        }

        override fun toString(): String = "PARTICLE_SHEET_SOFT_TRANSLUCENT"
    }
}
