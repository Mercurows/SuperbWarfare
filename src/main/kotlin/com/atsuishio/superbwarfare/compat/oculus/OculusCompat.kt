package com.atsuishio.superbwarfare.compat.oculus

import com.atsuishio.superbwarfare.compat.oculus.OculusCompat.getParticleShader
import com.atsuishio.superbwarfare.compat.oculus.OculusCompat.getParticleTranslucentShader
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.fml.ModList

/**
 * Reflection-based bridge to Oculus/Iris internal and public APIs.
 *
 * All calls are guarded with try/catch so the mod loads safely when Oculus is
 * absent — no compile-time dependency on Iris classes is required.
 *
 * ## Why reflection?
 *
 * Oculus is a client-only mod.  Making it a hard `implementation` dependency
 * would prevent the mod from loading on servers and on clients that don't
 * install Oculus.  Reflection keeps the dependency optional while still
 * giving us access to Iris internals that are *not* exposed through the
 * public [net.irisshaders.iris.api.v0.IrisApi].
 *
 * Key internal class used:
 * - [net.irisshaders.iris.pipeline.programs.ShaderAccess] — exposes the
 *   shader-pack-compiled particle programs ([getParticleTranslucentShader],
 *   [getParticleShader]) that replace vanilla core shaders when a pack is active.
 */
@OnlyIn(Dist.CLIENT)
object OculusCompat {

    /** True when Oculus or Iris mod id is present in the mod list. */
    val isInstalled: Boolean by lazy {
        ModList.get().isLoaded("oculus") || ModList.get().isLoaded("iris")
    }

    // -----------------------------------------------------------------------
    // Iris PUBLIC API (net.irisshaders.iris.api.v0.IrisApi)
    // -----------------------------------------------------------------------

    /**
     * Returns `true` when a shader pack is loaded **and** actively rendering.
     * Returns `false` when Oculus is installed but no pack is selected, or
     * when Oculus is not installed at all.
     */
    fun isShaderPackActive(): Boolean {
        if (!isInstalled) return false
        return try {
            val apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi")
            val instance = apiClass.getMethod("getInstance").invoke(null)
            apiClass.getMethod("isShaderPackInUse").invoke(instance) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Returns `true` when Iris is currently executing a **shadow pass**.
     * During shadow rendering, translucent particles should typically be
     * skipped to avoid writing incorrect depth / colour into the shadow map.
     */
    fun isRenderingShadowPass(): Boolean {
        if (!isInstalled) return false
        return try {
            val apiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi")
            val instance = apiClass.getMethod("getInstance").invoke(null)
            apiClass.getMethod("isRenderingShadowPass").invoke(instance) as? Boolean ?: false
        } catch (_: Exception) {
            false
        }
    }

    // -----------------------------------------------------------------------
    // Iris INTERNAL: ShaderAccess — shader pack's own compiled programs
    // -----------------------------------------------------------------------

    /**
     * Returns the shader pack's **translucent particle** [ShaderInstance],
     * or `null` if no pack is active or [ShaderAccess] is unavailable.
     *
     * This is the `gbuffers_particles_translucent` (or equivalent) program
     * from the active shader pack — it typically does **not** include
     * Minecraft's hard `if (color.a < 0.1) discard;` threshold.
     */
    fun getParticleTranslucentShader(): ShaderInstance? {
        if (!isInstalled) return null
        return try {
            val cls = Class.forName("net.irisshaders.iris.pipeline.programs.ShaderAccess")
            val method = cls.getMethod("getParticleTranslucentShader")
            method.invoke(null) as? ShaderInstance
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns the shader pack's **opaque particle** [ShaderInstance],
     * or `null` if no pack is active or [ShaderAccess] is unavailable.
     *
     * Fallback for when [getParticleTranslucentShader] returns `null`.
     */
    fun getParticleShader(): ShaderInstance? {
        if (!isInstalled) return null
        return try {
            val cls = Class.forName("net.irisshaders.iris.pipeline.programs.ShaderAccess")
            val method = cls.getMethod("getParticleShader")
            method.invoke(null) as? ShaderInstance
        } catch (_: Exception) {
            null
        }
    }
}
