package com.atsuishio.superbwarfare.client.renderer.scope

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.neoforge.client.event.RegisterShadersEvent
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL12
import org.lwjgl.opengl.GL13
import kotlin.math.abs
import kotlin.math.exp

@OnlyIn(Dist.CLIENT)
object ScopeLensShader {

    /** Same falloff the fragment shader uses, for the vertex-colour fallback. */
    const val EDGE_BLUR = 0.155f

    private var shader: ShaderInstance? = null

    private var screenTexture = -1
    private var screenWidth = -1
    private var screenHeight = -1

    fun onRegisterShaders(event: RegisterShadersEvent) {
        event.registerShader(
            ShaderInstance(
                event.resourceProvider,
                ResourceLocation.fromNamespaceAndPath(Mod.MODID, "rendertype_scope_lens"),
                DefaultVertexFormat.NEW_ENTITY
            )
        ) { shader = it }
    }

    internal fun captureScreen(): Boolean {
        val target = Minecraft.getInstance().mainRenderTarget
        val width = target.width
        val height = target.height
        if (width <= 0 || height <= 0) return false

        if (screenTexture < 0) {
            screenTexture = GlStateManager._genTexture()
        }
        GlStateManager._activeTexture(GL13.GL_TEXTURE0)
        GlStateManager._bindTexture(screenTexture)
        if (width != screenWidth || height != screenHeight) {
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE)
            GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE)
            GlStateManager._texImage2D(
                GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8,
                width, height, 0, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, null
            )
            screenWidth = width
            screenHeight = height
        }
        GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 0, 0, width, height)
        RenderSystem.setShaderTexture(0, screenTexture)
        return true
    }

    fun isAvailable(): Boolean = shader != null

    private fun lensTint(color: Int, strength: Float): FloatArray {
        val s = strength.coerceIn(0f, 1f)
        if (s <= 0f) return floatArrayOf(1f, 1f, 1f)
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        return floatArrayOf(
            1f + (r - 1f) * s,
            1f + (g - 1f) * s,
            1f + (b - 1f) * s
        )
    }

    fun screenScale(): Float {
        if (DisplayConfig.LENS_ZOOM_RATIO.get() <= 0.0) return 1f
        val world = ClientEventHandler.projectionMatrix ?: return 1f
        val handM11 = RenderSystem.getProjectionMatrix().m11()
        if (abs(handM11) < 1.0e-6f) return 1f
        return (world.m11() / handM11).coerceIn(0.1f, 10f)
    }

    fun beginSurface(
        centerX: Float,
        centerY: Float,
        radius: Float,
        edgeWidth: Float,
        magnification: Float,
        tintColor: Int,
        tintStrength: Float,
        screenScale: Float = screenScale(),
        thermal: Boolean = false,
        thermalBackgroundColor: Int? = null,
        thermalEntityColor: Int? = null
    ): Boolean {
        if (radius <= 1.0e-4f) return false
        val custom = shader ?: return false
        if (!captureScreen()) return false
        RenderSystem.setShader { custom }
        custom.safeGetUniform("LensCenter").set(centerX, centerY)
        custom.safeGetUniform("LensRadius").set(radius)
        custom.safeGetUniform("LensEdgeWidth").set(edgeWidth)
        custom.safeGetUniform("LensMagnification").set(magnification.coerceAtLeast(0.001f))
        custom.safeGetUniform("LensScreenScale").set(screenScale)
        custom.safeGetUniform("LensTexelSize").set(1f / screenWidth, 1f / screenHeight)
        custom.safeGetUniform("LensHardEdge").set(DisplayConfig.LENS_HARD_EDGE.get().toFloat())
        custom.safeGetUniform("ThermalEnabled").set(if (thermal) 1 else 0)
        ScopeThermalRenderer.applyColors(custom, thermalBackgroundColor, thermalEntityColor)
        if (thermal) RenderSystem.setShaderTexture(1, ScopeThermalRenderer.maskTexture())
        val tint = lensTint(tintColor, tintStrength)
        custom.safeGetUniform("LensTint").set(tint[0], tint[1], tint[2])
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        return true
    }

    fun edgeAlpha(distance: Float, edgeWidth: Float): Float {
        val blur = EDGE_BLUR * edgeWidth.coerceAtLeast(0.001f)
        val falloff = (1f - distance.coerceIn(0f, 1f)) / blur
        return exp(-(falloff * falloff))
    }

    fun endSurface() {
        RenderSystem.disableBlend()
        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
    }
}
