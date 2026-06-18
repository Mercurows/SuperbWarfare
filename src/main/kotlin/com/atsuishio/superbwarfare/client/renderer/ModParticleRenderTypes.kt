package com.atsuishio.superbwarfare.client.renderer

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.compat.oculus.OculusCompat
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.particle.ParticleRenderType
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.client.renderer.texture.TextureAtlas
import net.minecraft.client.renderer.texture.TextureManager
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent

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
                ResourceLocation(Mod.MODID, "rendertype_particle_soft"),
                DefaultVertexFormat.PARTICLE
            )
        ) { shader -> softParticleShader = shader }
    }

    /**
     * A custom [ParticleRenderType] that provides soft-edged transparency by
     * removing the `if (color.a < 0.1) discard;` threshold found in the vanilla
     * particle fragment shader.
     *
     * ## Shader-mod compatibility (Oculus / Iris)
     *
     * Iris fundamentally ignores custom core shaders when a shader pack is
     * active — there is no way to force it to accept our `rendertype_particle_soft`.
     * Instead, this render type uses a precision fallback chain:
     *
     * | Condition                          | Shader used                                          |
     * |------------------------------------|------------------------------------------------------|
     * | Rendering a shadow pass            | [GameRenderer.getParticleShader] (opaque, safe)      |
     * | Shader pack active + ShaderAccess  | `ShaderAccess.getParticleTranslucentShader()`        |
     * | Shader pack active, no ShaderAccess| [GameRenderer.getParticleShader] (vanilla fallback)  |
     * | No shader pack                     | Custom `softParticleShader` (full soft transparency) |
     *
     * **Why `ShaderAccess`?**  When a shader pack is active, Iris compiles the
     * pack's own `gbuffers_particles_translucent` program.  `ShaderAccess`
     * exposes that compiled program — and shader pack authors typically do
     * **not** use a hard discard threshold, so soft transparency is preserved
     * through the pack's native particle shader.
     */
    val PARTICLE_SHEET_SOFT_TRANSLUCENT: ParticleRenderType = object : ParticleRenderType {

        override fun begin(builder: BufferBuilder, textureManager: TextureManager) {
            // Match vanilla PARTICLE_SHEET_TRANSLUCENT call order exactly —
            // blend/depth state first, texture second, shader LAST.
            // Calling resolveShader() after blend/depth/texture is set ensures
            // that the shader's own blend mode (from JSON) takes final priority,
            // exactly mirroring vanilla's `RenderSystem.setShader(GameRenderer::getParticleShader)`.
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.depthMask(false)
            RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_PARTICLES)
            resolveShader()
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE)
        }

        override fun end(tesselator: Tesselator) {
            tesselator.end()
        }

        override fun toString(): String = "PARTICLE_SHEET_SOFT_TRANSLUCENT"

        // ------------------------------------------------------------------
        // Internal shader resolution
        // ------------------------------------------------------------------

        /**
         * Selects the best available shader for the current render context
         * and binds it via [RenderSystem.setShader].
         */
        private fun resolveShader() {
            // 1. Shadow pass — skip translucent rendering entirely.
            if (OculusCompat.isRenderingShadowPass()) {
                RenderSystem.setShader(GameRenderer::getParticleShader)
                return
            }

            // 2. Shader pack active — use the pack's native translucent particle
            //    program (which typically has no hard discard threshold).
            if (OculusCompat.isShaderPackActive()) {
                val oculusShader = OculusCompat.getParticleTranslucentShader()
                    ?: OculusCompat.getParticleShader()
                if (oculusShader != null) {
                    RenderSystem.setShader { oculusShader }
                } else {
                    // ShaderAccess unavailable (future Oculus refactor?) —
                    // fall back to the vanilla shader that Oculus intercepts.
                    RenderSystem.setShader(GameRenderer::getParticleShader)
                }
                return
            }

            // 3. No shader pack — use our custom no-discard soft shader.
            val custom = softParticleShader
            if (custom != null) {
                RenderSystem.setShader { custom }
            } else {
                // Should never happen, but ensures we never leave shader unset.
                RenderSystem.setShader(GameRenderer::getParticleShader)
            }
        }
    }
}
