package com.atsuishio.superbwarfare.client.renderer.scope

import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL30

/** Iris suppresses custom core-shader colour writes until its world composite has finished. */
@OnlyIn(Dist.CLIENT)
object ScopeLensPostProcessor {
    private data class Draw(val type: RenderType, var vertices: BufferBuilder.RenderedBuffer?)

    // Capture vertices now: the shared attachment model is reset before the composite runs.
    private class ReticleBuffers : MultiBufferSource {
        private val builders = linkedMapOf<RenderType, BufferBuilder>()
        private val active = linkedMapOf<RenderType, BufferBuilder>()

        override fun getBuffer(type: RenderType): VertexConsumer = active.getOrPut(type) {
            builders.getOrPut(type) { BufferBuilder(type.bufferSize()) }
                .also { it.begin(type.mode(), type.format()) }
        }

        fun finish(): List<Draw> = active.map { (type, builder) -> Draw(type, builder.end()) }
            .also { active.clear() }
    }

    private class Capture(val target: TextureTarget) {
        val reticle = ReticleBuffers()
        val surface = BufferBuilder(4096)
    }

    private data class Lens(
        val target: TextureTarget,
        val stencil: Int,
        val projection: Matrix4f,
        val modelView: Matrix4f,
        val screenScale: Float,
        val center: Vector3f,
        val edgeWidth: Float,
        val magnification: Float,
        val tint: Int,
        val tintStrength: Float,
        val thermal: Boolean,
        val thermalBackgroundColor: Int?,
        val thermalEntityColor: Int?,
        val shaderPack: Boolean,
        var surface: BufferBuilder.RenderedBuffer?,
        val reticle: List<Draw>
    )

    private val targets = mutableListOf<Capture>()
    private val lenses = mutableListOf<Lens>()

    @JvmStatic
    fun beginFrame() {
        // Also handles an interrupted frame, hidden hands and shader-pack switches.
        for (lens in lenses) {
            lens.surface?.release()
            lens.reticle.forEach { it.vertices?.release() }
        }
        lenses.clear()
    }

    /** Snapshot the live stencil before the gun renderer clears/reuses it. */
    fun defer(
        stencil: Int,
        center: Vector3f,
        edgeWidth: Float,
        magnification: Float,
        tint: Int,
        tintStrength: Float,
        surface: ((BufferBuilder) -> BufferBuilder.RenderedBuffer)?,
        reticle: (MultiBufferSource) -> Unit,
        thermal: Boolean = false,
        thermalBackgroundColor: Int? = null,
        thermalEntityColor: Int? = null
    ): Boolean {
        val shaderPack = ShaderCompat.isShaderPackActive()
        if ((!shaderPack && !thermal && !ScopeThermalRenderer.isFullScreenActive()) || ShaderCompat.isRenderingShadowPass() ||
            (surface != null && !ScopeLensShader.isAvailable()) ||
            (thermal && !ScopeThermalRenderer.isAvailable()) || !ScopeStencilRenderHelper.hasStencilAttachment()
        ) return false
        val main = Minecraft.getInstance().mainRenderTarget
        val drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
        val texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        val viewport = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
        if (viewport[0] != 0 || viewport[1] != 0 || viewport[2] != main.width || viewport[3] != main.height) return false
        val depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
        val scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
        val scissorBox = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox)
        val clearColor = BufferUtils.createFloatBuffer(4)
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor)
        try {
            RenderSystem.disableScissor()
            val slot = lenses.size
            val capture = if (slot < targets.size) targets[slot] else {
                val target = TextureTarget(main.width, main.height, true, Minecraft.ON_OSX)
                target.enableStencil()
                Capture(target).also { targets += it }
            }
            val target = capture.target
            if (target.frameBufferId < 0 || target.width != main.width || target.height != main.height) {
                target.resize(main.width, main.height, Minecraft.ON_OSX)
            }
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, drawFbo)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId)
            GlStateManager._glBlitFrameBuffer(
                0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL11.GL_DEPTH_BUFFER_BIT or GL11.GL_STENCIL_BUFFER_BIT, GL11.GL_NEAREST
            )
            val buffers = capture.reticle
            reticle(buffers)
            lenses += Lens(
                target, stencil, Matrix4f(RenderSystem.getProjectionMatrix()),
                Matrix4f(RenderSystem.getModelViewMatrix()), ScopeLensShader.screenScale(),
                Vector3f(center), edgeWidth, magnification, tint, tintStrength,
                thermal, thermalBackgroundColor, thermalEntityColor, shaderPack,
                surface?.invoke(capture.surface), buffers.finish()
            )
            return true
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo)
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
            RenderSystem.activeTexture(activeTexture)
            GlStateManager._bindTexture(texture)
            if (depthTest) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
            if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3])
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
        }
    }

    fun deferThermalSight(
        stencil: Int,
        backgroundColor: Int? = null,
        entityColor: Int? = null,
        magnification: Float = 1f,
        reticle: (MultiBufferSource) -> Unit
    ): Boolean = defer(
        stencil, Vector3f(), 0f, magnification, 0xFFFFFF, 0f, null, reticle,
        thermal = true, thermalBackgroundColor = backgroundColor, thermalEntityColor = entityColor
    )

    /** Called after AFTER_LEVEL, before vanilla clears hand depth and Iris converts colour space. */
    @JvmStatic
    fun render() {
        if (lenses.isEmpty()) return
        val main = Minecraft.getInstance().mainRenderTarget
        val drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val viewport = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
        val projection = Matrix4f(RenderSystem.getProjectionMatrix())
        val sorting = RenderSystem.getVertexSorting()
        val modelView = RenderSystem.getModelViewStack()
        val shader = RenderSystem.getShader()
        val shaderTexture = RenderSystem.getShaderTexture(0)
        val shaderTexture1 = RenderSystem.getShaderTexture(1)
        val activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
        RenderSystem.activeTexture(GL13.GL_TEXTURE0)
        val texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        val depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
        val depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
        val blend = GL11.glIsEnabled(GL11.GL_BLEND)
        val blendSrc = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB)
        val blendDst = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB)
        val blendSrcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA)
        val blendDstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA)
        val stencilTest = GL11.glIsEnabled(GL11.GL_STENCIL_TEST)
        val stencilFunc = GL11.glGetInteger(GL11.GL_STENCIL_FUNC)
        val stencilRef = GL11.glGetInteger(GL11.GL_STENCIL_REF)
        val stencilMask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK)
        val stencilFail = GL11.glGetInteger(GL11.GL_STENCIL_FAIL)
        val stencilDepthFail = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL)
        val stencilPass = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS)
        val cull = GL11.glIsEnabled(GL11.GL_CULL_FACE)
        val scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
        val scissorBox = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox)
        val clearColor = BufferUtils.createFloatBuffer(4)
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor)
        modelView.pushPose()
        try {
            RenderSystem.disableScissor()
            if (lenses.any { it.thermal }) ScopeThermalRenderer.renderMask()
            for (lens in lenses) {
                // A resize or pack switch must not reuse geometry/stencil from another framebuffer.
                if (ShaderCompat.isShaderPackActive() != lens.shaderPack || lens.target.width != main.width ||
                    lens.target.height != main.height
                ) continue
                GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, lens.target.frameBufferId)
                // Use the saved stencil without overwriting the main target's depth/stencil.
                GlStateManager._glFramebufferTexture2D(
                    GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, main.colorTextureId, 0
                )
                try {
                    RenderSystem.viewport(0, 0, main.width, main.height)
                    RenderSystem.setProjectionMatrix(lens.projection, VertexSorting.DISTANCE_TO_ORIGIN)
                    modelView.setIdentity()
                    modelView.mulPoseMatrix(lens.modelView)
                    RenderSystem.applyModelViewMatrix()
                    GL11.glEnable(GL11.GL_STENCIL_TEST)
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, lens.stencil, 0xFF)
                    RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
                    val center = lens.center
                    if (lens.surface == null && lens.thermal) {
                        ScopeThermalRenderer.drawSurface(
                            lens.thermalBackgroundColor, lens.thermalEntityColor, lens.magnification
                        )
                    } else if (ScopeLensShader.beginSurface(
                            center.x, center.y, center.z, lens.edgeWidth, lens.magnification,
                            lens.tint, lens.tintStrength, lens.screenScale, lens.thermal,
                            lens.thermalBackgroundColor, lens.thermalEntityColor
                        )
                    ) {
                        val vertices = lens.surface
                        lens.surface = null // BufferUploader owns/release()s it from here.
                        BufferUploader.drawWithShader(vertices!!)
                        ScopeLensShader.endSurface()
                    }
                    // Draw the original, unmagnified reticle over the finished lens.
                    for (draw in lens.reticle) {
                        draw.type.setupRenderState()
                        RenderSystem.disableDepthTest()
                        RenderSystem.depthMask(false)
                        val vertices = draw.vertices
                        draw.vertices = null
                        BufferUploader.drawWithShader(vertices!!)
                        draw.type.clearRenderState()
                    }
                } finally {
                    GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, lens.target.frameBufferId)
                    GlStateManager._glFramebufferTexture2D(
                        GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D,
                        lens.target.colorTextureId, 0
                    )
                }
            }
        } finally {
            // Release only buffers that were skipped; uploaded buffers have already been released.
            beginFrame()
            modelView.popPose()
            RenderSystem.applyModelViewMatrix()
            RenderSystem.setProjectionMatrix(projection, sorting)
            RenderSystem.setShader { shader }
            RenderSystem.setShaderTexture(0, shaderTexture)
            RenderSystem.setShaderTexture(1, shaderTexture1)
            RenderSystem.activeTexture(GL13.GL_TEXTURE0)
            GlStateManager._bindTexture(texture)
            RenderSystem.activeTexture(activeTexture)
            if (depthTest) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
            RenderSystem.depthMask(depthMask)
            if (blend) RenderSystem.enableBlend() else RenderSystem.disableBlend()
            RenderSystem.blendFuncSeparate(blendSrc, blendDst, blendSrcAlpha, blendDstAlpha)
            if (cull) RenderSystem.enableCull() else RenderSystem.disableCull()
            if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3])
            RenderSystem.stencilFunc(stencilFunc, stencilRef, stencilMask)
            RenderSystem.stencilOp(stencilFail, stencilDepthFail, stencilPass)
            if (stencilTest) GL11.glEnable(GL11.GL_STENCIL_TEST) else GL11.glDisable(GL11.GL_STENCIL_TEST)
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo)
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
        }
    }

    fun close() {
        beginFrame()
        targets.forEach { it.target.destroyBuffers() }
        // Keep the reusable BufferBuilders: their native backing storage has no GC cleaner.
    }
}
