package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeThermalRenderer
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.logging.LogUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.*
import kotlin.math.ceil
import kotlin.math.floor

/** 在世界合成后、镜片与热成像之前补光。场景只复制一次，各光源仅绘制覆盖的屏幕区域。 */
@OnlyIn(Dist.CLIENT)
object GunMuzzleLightRenderer {

    private val logger = LogUtils.getLogger()
    private var shader: ShaderInstance? = null
    private var scene: TextureTarget? = null
    private var view: Matrix4f? = null
    private var projection: Matrix4f? = null
    private var failed = false

    fun onRegisterShaders(event: RegisterShadersEvent) {
        event.registerShader(ShaderInstance(event.resourceProvider,
            ResourceLocation(Mod.MODID, "gun_muzzle_light"), DefaultVertexFormat.POSITION)) {
            shader = it
            failed = false
            close()
        }
    }

    @JvmStatic
    fun beginFrame() {
        view = null
        projection = null
    }

    @JvmStatic
    fun captureWorld(modelView: Matrix4f, worldProjection: Matrix4f) {
        if (ShaderCompat.isRenderingShadowPass()) return
        view = Matrix4f(modelView)
        projection = Matrix4f(worldProjection)
    }

    @JvmStatic
    fun render() {
        val custom = shader ?: return
        val worldView = view ?: return
        var worldProjection = projection ?: return
        if (failed || ShaderCompat.isRenderingShadowPass() || ScopeThermalRenderer.isFullScreenActive()) return
        val lights = GunMuzzleLightHandler.visibleLights()
        if (lights.isEmpty()) return
        val mc = Minecraft.getInstance()
        val main = mc.mainRenderTarget
        if (main.width <= 0 || main.height <= 0 || main.depthTextureId <= 0) return
        val shaderPack = ShaderCompat.isShaderPackActive()
        val worldState = if (shaderPack) ShaderCompat.getPostProcessWorldState() ?: return else null
        if (worldState != null) worldProjection = worldState.first
        if (worldState != null && !GL11.glIsTexture(worldState.second)) return

        val previousShader = RenderSystem.getShader()
        val program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)
        val drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val viewport = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
        // Do not write into an auxiliary view or a differently sized render target.
        if (viewport[0] != 0 || viewport[1] != 0 || viewport[2] != main.width || viewport[3] != main.height) return
        val depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
        val depthWrite = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
        val blend = GL11.glIsEnabled(GL11.GL_BLEND)
        val cull = GL11.glIsEnabled(GL11.GL_CULL_FACE)
        val stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST)
        val scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
        val scissorBox = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox)
        val colorMask = BufferUtils.createByteBuffer(4)
        GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK, colorMask)
        val blendSrc = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB)
        val blendDst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB)
        val blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA)
        val blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA)
        val blendEquationRgb = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB)
        val blendEquationAlpha = GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA)
        val activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
        val textures = IntArray(3) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + it)
            GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        }
        val capabilities = GL.getCapabilities()
        val samplers = if (capabilities.OpenGL33 || capabilities.GL_ARB_sampler_objects)
            IntArray(3) { GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, it) } else null
        val clearColor = BufferUtils.createFloatBuffer(4)
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor)
        val clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE)
        try {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0)
            RenderSystem.disableScissor()
            GL11.glDisable(GL11.GL_STENCIL_TEST)
            RenderSystem.colorMask(true, true, true, true)
            var copy = scene
            if (copy == null || copy.width != main.width || copy.height != main.height ||
                copy.isStencilEnabled != main.isStencilEnabled) {
                copy?.destroyBuffers()
                copy = TextureTarget(main.width, main.height, true, Minecraft.ON_OSX)
                scene = copy
                if (main.isStencilEnabled) copy.enableStencil()
            }
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId)
            GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST)
            main.bindWrite(true)
            samplers?.indices?.forEach { GL33.glBindSampler(it, 0) }
            custom.setSampler("SceneColor", copy.colorTextureId)
            custom.setSampler("SceneDepth", copy.depthTextureId)
            custom.setSampler("WorldDepth", worldState?.second ?: copy.depthTextureId)
            custom.safeGetUniform("InverseProjection").set(Matrix4f(worldProjection).invert())
            custom.safeGetUniform("InverseView").set(Matrix4f(worldView).invert())
            custom.safeGetUniform("Projection").set(worldProjection)
            custom.safeGetUniform("View").set(worldView)
            custom.safeGetUniform("TexelSize").set(1f / main.width, 1f / main.height)
            custom.safeGetUniform("ShaderPackMode").set(if (shaderPack) 1 else 0)
            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.disableCull()
            RenderSystem.enableBlend()
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD)
            RenderSystem.blendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE)
            RenderSystem.setShader { custom }
            val camera = mc.gameRenderer.mainCamera.position
            for (light in lights) {
                val point = light.position.subtract(camera)
                val bounds = screenBounds(worldView, worldProjection, point.x.toFloat(), point.y.toFloat(),
                    point.z.toFloat(), light.radius, main.width, main.height) ?: continue
                RenderSystem.enableScissor(bounds[0], bounds[1], bounds[2], bounds[3])
                custom.safeGetUniform("LightPosition").set(point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
                custom.safeGetUniform("LightDirection").set(light.direction.x.toFloat(), light.direction.y.toFloat(), light.direction.z.toFloat())
                custom.safeGetUniform("LightRadius").set(light.radius)
                custom.safeGetUniform("LightStrength").set(GunMuzzleLightHandler.intensity(light))
                val builder = Tesselator.getInstance().builder
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
                builder.vertex(-1.0, -1.0, 0.0).endVertex()
                builder.vertex(1.0, -1.0, 0.0).endVertex()
                builder.vertex(1.0, 1.0, 0.0).endVertex()
                builder.vertex(-1.0, 1.0, 0.0).endVertex()
                BufferUploader.drawWithShader(builder.end())
            }
        } catch (exception: Exception) {
            failed = true
            close()
            logger.warn("Gun muzzle environment lighting disabled until resource reload", exception)
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo)
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo)
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
            RenderSystem.colorMask(colorMask[0].toInt() != 0, colorMask[1].toInt() != 0,
                colorMask[2].toInt() != 0, colorMask[3].toInt() != 0)
            RenderSystem.depthMask(depthWrite)
            if (depthTest) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
            if (cull) RenderSystem.enableCull() else RenderSystem.disableCull()
            if (stencil) GL11.glEnable(GL11.GL_STENCIL_TEST) else GL11.glDisable(GL11.GL_STENCIL_TEST)
            if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3])
            else RenderSystem.disableScissor()
            GL20.glBlendEquationSeparate(blendEquationRgb, blendEquationAlpha)
            RenderSystem.blendFuncSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha)
            if (blend) RenderSystem.enableBlend() else RenderSystem.disableBlend()
            textures.forEachIndexed { unit, texture ->
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit)
                GlStateManager._bindTexture(texture)
            }
            samplers?.forEachIndexed { unit, sampler -> GL33.glBindSampler(unit, sampler) }
            RenderSystem.activeTexture(activeTexture)
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
            RenderSystem.clearDepth(clearDepth)
            RenderSystem.setShader { previousShader }
            GlStateManager._glUseProgram(program)
        }
    }

    /** 投影包围盒而非枪口中心，因此屏幕外枪口仍能照到画面内的墙。近裁剪相交时保守覆盖全屏。 */
    private fun screenBounds(view: Matrix4f, projection: Matrix4f, x: Float, y: Float, z: Float,
                             radius: Float, width: Int, height: Int): IntArray? {
        val center = view.transformPosition(Vector3f(x, y, z))
        if (center.z > radius) return null
        if (center.z + radius >= -0.05f) return intArrayOf(0, 0, width, height)
        var minX = 1f
        var minY = 1f
        var maxX = -1f
        var maxY = -1f
        for (dx in listOf(-radius, radius)) for (dy in listOf(-radius, radius)) for (dz in listOf(-radius, radius)) {
            val p = projection.transform(Vector4f(center.x + dx, center.y + dy, center.z + dz, 1f))
            minX = minOf(minX, p.x / p.w)
            minY = minOf(minY, p.y / p.w)
            maxX = maxOf(maxX, p.x / p.w)
            maxY = maxOf(maxY, p.y / p.w)
        }
        val left = floor((minX * 0.5f + 0.5f) * width).toInt().coerceIn(0, width)
        val bottom = floor((minY * 0.5f + 0.5f) * height).toInt().coerceIn(0, height)
        val right = ceil((maxX * 0.5f + 0.5f) * width).toInt().coerceIn(0, width)
        val top = ceil((maxY * 0.5f + 0.5f) * height).toInt().coerceIn(0, height)
        return if (left >= right || bottom >= top) null else intArrayOf(left, bottom, right - left, top - bottom)
    }

    @JvmStatic
    fun close() {
        scene?.destroyBuffers()
        scene = null
    }
}
