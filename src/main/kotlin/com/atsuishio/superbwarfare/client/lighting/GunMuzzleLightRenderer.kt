package com.atsuishio.superbwarfare.client.lighting

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.renderer.scope.ScopeThermalRenderer
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.logging.LogUtils
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.ClipContext
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.*
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.floor

/** 在世界合成后、镜片与热成像之前补光。场景只复制一次，各光源仅绘制覆盖的屏幕区域。 */
@OnlyIn(Dist.CLIENT)
object GunMuzzleLightRenderer {

    private const val FLASHLIGHT_BEAM_LENGTH = 2.0
    private const val SURFACE_PASS = 0
    private const val FLASHLIGHT_EFFECTS_PASS = 1
    private const val COMPOSITE_PASS = 2
    private val shadowUnpackParameters = intArrayOf(
        GL11.GL_UNPACK_ROW_LENGTH, GL11.GL_UNPACK_SKIP_ROWS, GL11.GL_UNPACK_SKIP_PIXELS, GL11.GL_UNPACK_SWAP_BYTES
    )

    private class FlashlightVisibility(var value: Float, var updatedAt: Long)
    private class LightDraw(
        val light: GunMuzzleLightHandler.Light,
        val point: Vec3,
        val bounds: IntArray,
        val strength: Float,
    )

    private val logger = LogUtils.getLogger()
    private val flashlightVisibility = mutableMapOf<String, FlashlightVisibility>()
    private var shader: ShaderInstance? = null
    private var scene: TextureTarget? = null
    private var flashlightEffects: TextureTarget? = null
    private var view: Matrix4f? = null
    private var inverseWorldPose: Matrix4f? = null
    private var projection: Matrix4f? = null
    private var failed = false

    @JvmStatic
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
        inverseWorldPose = null
        projection = null
        FlashlightHandler.beginFrame()
    }

    @JvmStatic
    fun captureWorld(worldPose: Matrix4f, worldProjection: Matrix4f) {
        if (ShaderCompat.isRenderingShadowPass()) return
        inverseWorldPose = Matrix4f(worldPose).invert()
        view = Matrix4f(RenderSystem.getModelViewMatrix()).mul(worldPose)
        projection = Matrix4f(worldProjection)
    }

    @JvmStatic
    fun worldPoseInverse(): Matrix4f? = inverseWorldPose

    @JvmStatic
    fun render() {
        val custom = shader ?: return
        val worldView = view ?: return
        var worldProjection = projection ?: return
        if (failed || ShaderCompat.isRenderingShadowPass() || ScopeThermalRenderer.isFullScreenActive()) return
        val lights = GunMuzzleLightHandler.visibleLights() + FlashlightHandler.visibleLights() +
            WorldLightHandler.visibleLights()
        if (lights.isEmpty()) return
        val now = Util.getMillis()
        flashlightVisibility.values.removeIf { now - it.updatedAt > 250L }
        val mc = Minecraft.getInstance()
        val main = mc.mainRenderTarget
        if (main.width <= 0 || main.height <= 0 || main.depthTextureId <= 0) return
        val shaderPack = ShaderCompat.isShaderPackActive()
        val worldState = if (shaderPack) ShaderCompat.getPostProcessWorldState() ?: return else null
        if (worldState != null) worldProjection = worldState.first
        if (worldState != null && !GL11.glIsTexture(worldState.second)) return

        val program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM)
        val drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val viewport = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
        // Require the completed main scene, including when another renderer uses a same-size target.
        if (drawFbo != main.frameBufferId || viewport[0] != 0 || viewport[1] != 0 ||
            viewport[2] != main.width || viewport[3] != main.height
        ) return
        val camera = mc.gameRenderer.mainCamera.position
        val lightTime = (mc.level?.gameTime ?: 0L).toDouble() + mc.frameTime
        val strengthScale = DisplayConfig.MUZZLE_LIGHT_STRENGTH.get().toFloat()
        val draws = lights.mapNotNull { light ->
            val strength = light.intensity(lightTime) * strengthScale
            if (strength <= 0f) return@mapNotNull null
            val point = light.position.subtract(camera)
            val bounds = screenBounds(worldView, worldProjection, point.x.toFloat(), point.y.toFloat(),
                point.z.toFloat(), light.radius, main.width, main.height) ?: return@mapNotNull null
            LightDraw(light, point, bounds, strength)
        }
        if (draws.isEmpty()) return
        FlashlightShadowMap.update(draws.map { it.light })
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
        val textures = IntArray(4) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + it)
            GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        }
        val capabilities = GL.getCapabilities()
        val samplers = if (capabilities.OpenGL33 || capabilities.GL_ARB_sampler_objects)
            IntArray(textures.size) { GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, it) } else null
        val unpackState = if (draws.any { it.light.flashlight != null })
            shadowUnpackParameters.map { GL11.glGetInteger(it) } else null
        val unpackBuffer = if (unpackState != null) GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING) else 0
        val clearColor = BufferUtils.createFloatBuffer(4)
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor)
        val clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE)
        try {
            // Atlas uploads can leave a row stride and offsets in the shared OpenGL state.
            if (unpackState != null) shadowUnpackParameters.forEach { GL11.glPixelStorei(it, 0) }
            if (unpackBuffer != 0) GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0)
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
            val effects = if (draws.any { it.light.flashlight?.showEffects == true }) {
                flashlightEffectsTarget(main.width, main.height)
            } else null
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
            RenderSystem.colorMask(true, true, true, false)
            for (draw in draws) {
                val light = draw.light
                val point = draw.point
                val bounds = draw.bounds
                RenderSystem.enableScissor(bounds[0], bounds[1], bounds[2], bounds[3])
                custom.safeGetUniform("LightPosition").set(point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
                custom.safeGetUniform("LightDirection").set(light.direction.x.toFloat(), light.direction.y.toFloat(), light.direction.z.toFloat())
                custom.safeGetUniform("LightRadius").set(light.radius)
                custom.safeGetUniform("LightStrength").set(draw.strength)
                custom.safeGetUniform("LightCone").set(light.coneCosOuter, light.coneCosInner)
                custom.safeGetUniform("LightColor").set(
                    light.color.x.toFloat(), light.color.y.toFloat(), light.color.z.toFloat()
                )
                custom.safeGetUniform("LightShadow").set(light.shadowThickness)
                custom.safeGetUniform("MuzzleFlash").set(if (light.muzzleFlash) 1 else 0)
                custom.safeGetUniform("LightSelfMask").set(light.selfMask)
                if (!FlashlightShadowMap.bind(custom, light, camera)) continue
                draw(custom, SURFACE_PASS, GL14.GL_FUNC_ADD)

                val emitter = light.flashlight
                if (effects != null && emitter?.showEffects == true) {
                    val beamEnd = light.position.add(light.direction.scale(minOf(FLASHLIGHT_BEAM_LENGTH, light.radius.toDouble())))
                    val beamHit = clipFlashlight(light, beamEnd)
                    val beamLength = light.position.distanceTo(beamHit.location).toFloat()
                    val lensVisible = point.lengthSqr() < light.radius * light.radius &&
                        clipFlashlight(light, camera).type == HitResult.Type.MISS
                    custom.safeGetUniform("FlashlightEffect").set(
                        beamLength, smoothFlashlightVisibility(emitter.key, lensVisible, now)
                    )
                    // 重合像素只保留各通道的最大亮度。
                    effects.bindWrite(false)
                    draw(custom, FLASHLIGHT_EFFECTS_PASS, GL14.GL_MAX)
                    main.bindWrite(false)
                }
            }
            if (effects != null) {
                RenderSystem.disableScissor()
                custom.setSampler("SceneColor", effects.colorTextureId)
                draw(custom, COMPOSITE_PASS, GL14.GL_FUNC_ADD)
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
            // Restore both the Blaze3D cache and the state set directly for the draw.
            RenderSystem.blendFuncSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha)
            GL14.glBlendFuncSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha)
            if (blend) {
                RenderSystem.enableBlend()
                GL11.glEnable(GL11.GL_BLEND)
            } else {
                RenderSystem.disableBlend()
                GL11.glDisable(GL11.GL_BLEND)
            }
            textures.forEachIndexed { unit, texture ->
                RenderSystem.activeTexture(GL13.GL_TEXTURE0 + unit)
                GlStateManager._bindTexture(texture)
            }
            samplers?.forEachIndexed { unit, sampler -> GL33.glBindSampler(unit, sampler) }
            unpackState?.forEachIndexed { index, value -> GL11.glPixelStorei(shadowUnpackParameters[index], value) }
            if (unpackBuffer != 0) GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer)
            RenderSystem.activeTexture(activeTexture)
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
            RenderSystem.clearDepth(clearDepth)
            GlStateManager._glUseProgram(program)
        }
    }

    private fun flashlightEffectsTarget(width: Int, height: Int): TextureTarget {
        var target = flashlightEffects
        if (target == null || target.width != width || target.height != height) {
            target?.destroyBuffers()
            target = TextureTarget(width, height, false, Minecraft.ON_OSX)
            flashlightEffects = target
        }
        target.bindWrite(false)
        RenderSystem.clearColor(0f, 0f, 0f, 0f)
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX)
        return target
    }

    private fun draw(shader: ShaderInstance, pass: Int, blendEquation: Int) {
        shader.safeGetUniform("Pass").set(pass)
        try {
            shader.apply()
            // ShaderInstance caches its blend mode, so set the equation after apply().
            GL11.glEnable(GL11.GL_BLEND)
            GL20.glBlendEquationSeparate(blendEquation, GL14.GL_FUNC_ADD)
            GL14.glBlendFuncSeparate(GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO, GL11.GL_ONE)
            val builder = Tesselator.getInstance().builder
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
            builder.vertex(-1.0, -1.0, 0.0).endVertex()
            builder.vertex(1.0, -1.0, 0.0).endVertex()
            builder.vertex(1.0, 1.0, 0.0).endVertex()
            builder.vertex(-1.0, 1.0, 0.0).endVertex()
            BufferUploader.draw(builder.end())
        } finally {
            shader.clear()
        }
    }

    private fun smoothFlashlightVisibility(key: String, visible: Boolean, now: Long): Float {
        val target = if (visible) 1f else 0f
        val state = flashlightVisibility.getOrPut(key) { FlashlightVisibility(target, now) }
        // 方块边缘会使 raycast 在命中和未命中之间跳变，让可见度在几帧内过渡。
        val elapsed = (now - state.updatedAt).coerceAtLeast(0L)
        val fraction = (1.0 - exp(-elapsed / 45.0)).toFloat()
        state.value += (target - state.value) * fraction
        state.updatedAt = now
        return state.value
    }

    @JvmStatic
    private fun clipFlashlight(light: GunMuzzleLightHandler.Light, end: Vec3): HitResult =
        light.holder.level().clip(
            ClipContext(light.position, end, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, light.holder)
        )

    /** 投影包围盒而非枪口中心，因此屏幕外枪口仍能照到画面内的墙。近裁剪相交时保守覆盖全屏。 */
    @JvmStatic
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
        flashlightVisibility.clear()
        FlashlightShadowMap.close()
        scene?.destroyBuffers()
        scene = null
        flashlightEffects?.destroyBuffers()
        flashlightEffects = null
    }
}
