package com.atsuishio.superbwarfare.compat.shader

import com.atsuishio.superbwarfare.compat.shader.ShaderCompat.getParticleTranslucentShader
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.ShaderInstance
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.fml.ModList
import org.joml.Matrix4f

/**
 * Reflection-based bridge to Oculus/Iris internal and public APIs
 */
@OnlyIn(Dist.CLIENT)
object ShaderCompat {

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
    @JvmStatic
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
    @JvmStatic
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

    /** Projection and depthtex2 after Iris/Oculus has finished its world composite passes. */
    fun getPostProcessWorldState(requireCompositeComplete: Boolean = true): Pair<Matrix4f, Int>? {
        if (!isInstalled || isRenderingShadowPass()) return null
        return try {
            val iris = Class.forName("net.irisshaders.iris.Iris")
            val manager = iris.getMethod("getPipelineManager").invoke(null)
            val pipeline = manager.javaClass.getMethod("getPipelineNullable").invoke(manager) ?: return null
            val shaderPipeline = Class.forName("net.irisshaders.iris.pipeline.ShaderRenderingPipeline")
            if (!shaderPipeline.isInstance(pipeline) ||
                (requireCompositeComplete && shaderPipeline.getMethod("shouldOverrideShaders").invoke(pipeline) != false)
            ) return null // Custom core shaders are suppressed until world rendering has ended.

            val targetsField = pipeline.javaClass.getDeclaredField("renderTargets")
            if (!targetsField.trySetAccessible()) return null
            val targets = targetsField.get(pipeline) ?: return null
            val noHand = targets.javaClass.getMethod("getDepthTextureNoHand").invoke(targets) ?: return null
            val depthTexture = noHand.javaClass.getMethod("getTextureId").invoke(noHand) as? Int ?: return null
            if (depthTexture <= 0) return null

            val stateClass = Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState")
            val state = stateClass.getField("INSTANCE").get(null)
            val projection = stateClass.getMethod("getGbufferProjection").invoke(state) as? Matrix4f ?: return null
            Matrix4f(projection) to depthTexture
        } catch (_: ReflectiveOperationException) {
            null
        } catch (_: RuntimeException) {
            null
        } catch (_: LinkageError) {
            null
        }
    }

    /**
     * Force-flushes a first-person item [MultiBufferSource.BufferSource] when Iris/Oculus
     * wraps it in its batched rendering pipeline. Vanilla item rendering normally works with
     * [MultiBufferSource.BufferSource.endBatch], but Iris may pass an unflushable wrapper whose
     * endBatch methods are no-ops; in that case we unwrap the private `wrapped` source and flush
     * the real [net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource].
     */
    fun endBatch(bufferSource: MultiBufferSource.BufferSource): Boolean {
        if (!isInstalled) return false
        return try {
            var target = bufferSource
            val wrapperClass = Class.forName(
                "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource\$UnflushableWrapper"
            )
            if (wrapperClass.isInstance(target)) {
                val wrappedField = wrapperClass.getDeclaredField("wrapped")
                wrappedField.isAccessible = true
                target = wrappedField.get(target) as MultiBufferSource.BufferSource
            }

            val fullyBufferedClass = Class.forName(
                "net.irisshaders.batchedentityrendering.impl.FullyBufferedMultiBufferSource"
            )
            if (fullyBufferedClass.isInstance(target)) {
                target.endBatch()
                true
            } else {
                false
            }
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
