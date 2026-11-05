package com.atsuishio.superbwarfare.client.renderer.scope

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.attachment.BedrockAttachmentModel
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.data.gun.GunData
import com.atsuishio.superbwarfare.data.gun.value.AttachmentType
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.item.gun.GunItem
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30

/** Copies the geometry of rendered living entities; it never changes their normal world render. */
@OnlyIn(Dist.CLIENT)
@net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = Mod.MODID,
    bus = net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ScopeThermalRenderer {
    private data class Batch(val outline: RenderType, val builder: BufferBuilder)
    private val builders = mutableMapOf<RenderType, Batch>()
    private val active = linkedMapOf<RenderType, Batch>()
    private data class HandStencil(val target: TextureTarget, val function: Int, val reference: Int, val valueMask: Int)
    private data class HandBatch(
        val batch: Batch, val projection: Matrix4f, val modelView: Matrix4f,
        val depthNear: Double, val depthFar: Double, val stencil: HandStencil?,
    )
    private val handBuilders = mutableListOf<BufferBuilder>()
    private val handStencilTargets = mutableListOf<TextureTarget>()
    private val activeHands = mutableListOf<HandBatch>()
    private var handBuildersUsed = 0
    private var handStencilTargetsUsed = 0
    private var enabled = false
    private var fullScreen = false
    private var fullScreenDrawn = false
    private var maskDrawn = false
    private var worldProjection: Matrix4f? = null
    private var worldModelView: Matrix4f? = null
    private var target: TextureTarget? = null
    private var worldDepth: TextureTarget? = null
    private var handDepth: TextureTarget? = null
    private var hasWorldDepth = false
    private var hasHandDepth = false
    private var maskShader: ShaderInstance? = null
    private var surfaceShader: ShaderInstance? = null
    private var visionShader: ShaderInstance? = null

    // Kotlin for Forge registers the object instance, so this listener must not be static.
    @SubscribeEvent
    fun onRegisterShaders(event: RegisterShadersEvent) {
        event.registerShader(ShaderInstance(event.resourceProvider,
            ResourceLocation(Mod.MODID, "rendertype_scope_thermal_mask"), DefaultVertexFormat.POSITION_COLOR_TEX)) {
            maskShader = it
        }
        event.registerShader(ShaderInstance(event.resourceProvider,
            ResourceLocation(Mod.MODID, "rendertype_scope_thermal"), DefaultVertexFormat.POSITION_TEX)) {
            surfaceShader = it
        }
        // 护目镜/载具热成像：黑白、无描边，保留像素化与噪点，和镜内的冷热配色区分开
        event.registerShader(ShaderInstance(event.resourceProvider,
            ResourceLocation(Mod.MODID, "rendertype_thermal_vision"), DefaultVertexFormat.POSITION_TEX)) {
            visionShader = it
        }
    }

    fun isAvailable(): Boolean = maskShader != null && surfaceShader != null && visionShader != null

    /** Lenses must magnify the composited scene and heat together, never the scene alone. */
    fun isFullScreenActive(): Boolean = enabled && fullScreen

    @JvmStatic
    fun beginFrame() {
        discardGeometry()
        worldProjection = null
        worldModelView = null
        hasWorldDepth = false
        hasHandDepth = false
        fullScreenDrawn = false
        maskDrawn = false
        val mc = Minecraft.getInstance()
        fullScreen = ClientEventHandler.activeThermalImaging
        val stack = mc.player?.mainHandItem
        val scope = mc.options.cameraType.isFirstPerson && stack?.item is GunItem &&
            ClientEventHandler.zoomTime >= BedrockAttachmentModel.DIVISION_MIN_ZOOM &&
            GunData.from(stack).let { gun ->
                gun.attachment.scopeStack().lastOrNull()?.definition?.scopeInfo
                    ?.mode(gun.attachment.scopeMode(AttachmentType.SCOPE))?.thermal == true
            }
        enabled = isAvailable() && (fullScreen || scope)
    }

    @JvmStatic
    fun renderFullScreen(): Boolean {
        if (!enabled || !fullScreen || fullScreenDrawn || ShaderCompat.isRenderingShadowPass()) return false
        fullScreenDrawn = true
        // 手与手持物是这一帧最后才画的，遮罩要用"含手"的深度才不会穿过去
        captureHandDepth()
        renderMask()
        if (!beginVisionSurface()) return false
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        builder.vertex(-1.0, -1.0, 0.0).uv(0f, 0f).endVertex()
        builder.vertex(1.0, -1.0, 0.0).uv(1f, 0f).endVertex()
        builder.vertex(1.0, 1.0, 0.0).uv(1f, 1f).endVertex()
        builder.vertex(-1.0, 1.0, 0.0).uv(0f, 1f).endVertex()
        BufferUploader.drawWithShader(builder.end())
        RenderSystem.disableBlend()
        RenderSystem.depthMask(true)
        RenderSystem.enableDepthTest()
        return true
    }

    /** Vanilla clears world depth before drawing hands, so preserve it before that clear. */
    @JvmStatic
    fun captureWorldDepth() {
        if (!enabled || ShaderCompat.isShaderPackActive()) return
        worldDepth = copyMainDepthTo(worldDepth)
        hasWorldDepth = worldDepth != null
    }

    /**
     * 复制**含第一人称手/手持物**的深度（[renderFullScreen] 画遮罩之前调用）。
     *
     * 手是 `renderLevel` 结束之后才画的，世界深度里没有它，热源遮罩会直接"穿透"手臂和枪；
     * 这里在遮罩之前再拷一份当前深度，着色器里逐像素挡掉被手遮住的热源。
     */
    @JvmStatic
    fun captureHandDepth() {
        if (!enabled || !fullScreen) return
        handDepth = copyMainDepthTo(handDepth)
        hasHandDepth = handDepth != null
    }

    /** Copy main depth, or the live arm draw's depth/stencil before the gun clears its clipping mask. */
    private fun copyMainDepthTo(current: TextureTarget?, stencil: Boolean = false): TextureTarget? {
        val main = Minecraft.getInstance().mainRenderTarget
        val drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val textureUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
        RenderSystem.activeTexture(GL13.GL_TEXTURE0)
        val texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        val depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
        val viewport = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
        val clearColor = BufferUtils.createFloatBuffer(4)
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor)
        val clearDepth = GL11.glGetDouble(GL11.GL_DEPTH_CLEAR_VALUE)
        val scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
        val scissorBox = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox)
        try {
            RenderSystem.disableScissor()
            val withStencil = stencil || main.isStencilEnabled
            val copy = if (current != null && current.isStencilEnabled == withStencil) current else {
                current?.destroyBuffers()
                TextureTarget(main.width, main.height, true, Minecraft.ON_OSX).also {
                    if (withStencil) it.enableStencil()
                }
            }
            if (copy.width != main.width || copy.height != main.height) copy.resize(main.width, main.height, Minecraft.ON_OSX)
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, if (stencil) drawFbo else main.frameBufferId)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId)
            GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL11.GL_DEPTH_BUFFER_BIT or (if (stencil) GL11.GL_STENCIL_BUFFER_BIT else 0), GL11.GL_NEAREST)
            return copy
        } catch (_: Throwable) {
            return null
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo)
            GlStateManager._bindTexture(texture)
            RenderSystem.activeTexture(textureUnit)
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
            RenderSystem.clearDepth(clearDepth)
            if (depthTest) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
            if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3])
        }
    }

    private val solidTexture = ResourceLocation(Mod.MODID, "textures/entity/white.png")
    // Same memoized type used by EntityRenderDispatcher; its ground quads are not heat sources.
    private val shadowType = RenderType.entityShadow(ResourceLocation("minecraft", "textures/misc/shadow.png"))
    private var solidType: RenderType? = null

    private fun solidMaskType(): RenderType =
        solidType ?: RenderType.entityCutoutNoCull(solidTexture).also { solidType = it }


    @JvmStatic
    fun capture(entity: Entity, source: MultiBufferSource): MultiBufferSource {
        val mc = Minecraft.getInstance()
        // Only hide the camera body in first person; third-person self uses the normal world heat mask.
        val hiddenCameraBody = entity === mc.cameraEntity && mc.options.cameraType.isFirstPerson
        if (!enabled || !entity.isAlive || entity.isInvisible || hiddenCameraBody || ShaderCompat.isRenderingShadowPass()
        ) return source
        return MultiBufferSource { type ->
            val original = source.getBuffer(type)
            // Dispatcher renders shadows through this same source after the entity model.
            // Never let the non-living solid-mask fallback turn a shadow into a white quad.
            if (type === shadowType) return@MultiBufferSource original
            val textured = type.format() == DefaultVertexFormat.NEW_ENTITY
            val fallback = if (entity is LivingEntity) null else solidMaskType()
            val outline = if (textured) type.outline().orElse(fallback) else null
            if (outline == null) original else {
                if (worldProjection == null) {
                    worldProjection = Matrix4f(RenderSystem.getProjectionMatrix())
                    worldModelView = Matrix4f(RenderSystem.getModelViewMatrix())
                }
                val batch = active.getOrPut(type) {
                    builders.getOrPut(type) { Batch(outline, BufferBuilder(4096)) }.also {
                        it.builder.begin(type.mode(), DefaultVertexFormat.POSITION_COLOR_TEX)
                    }
                }
                VertexMultiConsumer.create(original, MaskConsumer(batch.builder))
            }
        }
    }

    /** Capture only the player's arm/sleeve stream, never the surrounding held-item stream. */
    @JvmStatic
    fun captureFirstPersonArm(source: MultiBufferSource): MultiBufferSource {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: return source
        if (!isFullScreenActive() || maskDrawn || !mc.options.cameraType.isFirstPerson ||
            mc.cameraEntity !== player || player.isInvisible || ShaderCompat.isRenderingShadowPass()
        ) return source
        val captured = mutableMapOf<RenderType, HandBatch>()
        return MultiBufferSource { type ->
            val original = source.getBuffer(type)
            if (type === shadowType || type.format() != DefaultVertexFormat.NEW_ENTITY) {
                return@MultiBufferSource original
            }
            val outline = type.outline().orElse(null) ?: return@MultiBufferSource original
            val hand = captured.getOrPut(type) {
                // The real gun draw clips arms out of its optic with stencil == 0. Depth alone
                // cannot reproduce that: the empty lens has far depth, so hidden arms would pass.
                val stencil = if (GL11.glIsEnabled(GL11.GL_STENCIL_TEST) &&
                    GL11.glGetInteger(GL11.GL_STENCIL_FUNC) != GL11.GL_ALWAYS &&
                    ScopeStencilRenderHelper.hasStencilAttachment()
                ) {
                    val index = handStencilTargetsUsed
                    val copy = copyMainDepthTo(handStencilTargets.getOrNull(index), stencil = true)
                        ?: return@MultiBufferSource original // Never invent heat if clipping cannot be preserved.
                    if (index < handStencilTargets.size) handStencilTargets[index] = copy else handStencilTargets.add(copy)
                    handStencilTargetsUsed++
                    HandStencil(copy, GL11.glGetInteger(GL11.GL_STENCIL_FUNC),
                        GL11.glGetInteger(GL11.GL_STENCIL_REF), GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK))
                } else null
                val builder = if (handBuildersUsed < handBuilders.size) handBuilders[handBuildersUsed]
                    else BufferBuilder(4096).also { handBuilders.add(it) }
                handBuildersUsed++
                builder.begin(type.mode(), DefaultVertexFormat.POSITION_COLOR_TEX)
                val range = BufferUtils.createDoubleBuffer(2)
                GL11.glGetDoublev(GL11.GL_DEPTH_RANGE, range)
                HandBatch(Batch(outline, builder), Matrix4f(RenderSystem.getProjectionMatrix()),
                    Matrix4f(RenderSystem.getModelViewMatrix()), range[0], range[1], stencil).also { activeHands.add(it) }
            }
            VertexMultiConsumer.create(original, MaskConsumer(hand.batch.builder))
        }
    }

    private class MaskConsumer(private val delegate: VertexConsumer) : DefaultedVertexConsumer() {
        private var x = 0.0
        private var y = 0.0
        private var z = 0.0
        private var u = 0f
        private var v = 0f
        override fun vertex(x: Double, y: Double, z: Double): VertexConsumer {
            this.x = x; this.y = y; this.z = z
            return this
        }
        override fun color(r: Int, g: Int, b: Int, a: Int): VertexConsumer = this
        override fun uv(u: Float, v: Float): VertexConsumer { this.u = u; this.v = v; return this }
        override fun overlayCoords(u: Int, v: Int): VertexConsumer = this
        override fun uv2(u: Int, v: Int): VertexConsumer = this
        override fun normal(x: Float, y: Float, z: Float): VertexConsumer = this
        override fun defaultColor(r: Int, g: Int, b: Int, a: Int) {}
        override fun unsetDefaultColor() {}
        override fun endVertex() {
            delegate.vertex(x, y, z).color(255, 255, 255, 255).uv(u, v).endVertex()
        }
    }

    /** Runs after Iris's composite, when custom core shaders can write colour again. */
    fun renderMask() {
        if (maskDrawn) return
        val drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val viewport = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
        val projection = Matrix4f(RenderSystem.getProjectionMatrix())
        val sorting = RenderSystem.getVertexSorting()
        val modelView = RenderSystem.getModelViewStack()
        val shader = RenderSystem.getShader()
        val texture = RenderSystem.getShaderTexture(0)
        val activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
        val depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
        val depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
        val depthRange = BufferUtils.createDoubleBuffer(2)
        GL11.glGetDoublev(GL11.GL_DEPTH_RANGE, depthRange)
        val blend = GL11.glIsEnabled(GL11.GL_BLEND)
        val cull = GL11.glIsEnabled(GL11.GL_CULL_FACE)
        val stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST)
        val stencilFunction = GL11.glGetInteger(GL11.GL_STENCIL_FUNC)
        val stencilReference = GL11.glGetInteger(GL11.GL_STENCIL_REF)
        val stencilValueMask = GL11.glGetInteger(GL11.GL_STENCIL_VALUE_MASK)
        val stencilFail = GL11.glGetInteger(GL11.GL_STENCIL_FAIL)
        val stencilDepthFail = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_FAIL)
        val stencilPass = GL11.glGetInteger(GL11.GL_STENCIL_PASS_DEPTH_PASS)
        modelView.pushPose()
        try {
            renderMaskToTarget()
        } finally {
            // Both clear() and the geometry pass change the FBO. The following screen capture
            // must read the scene, not the heat mask (including when there are no heat sources).
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo)
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
            modelView.popPose()
            RenderSystem.applyModelViewMatrix()
            RenderSystem.setProjectionMatrix(projection, sorting)
            RenderSystem.setShader { shader }
            RenderSystem.setShaderTexture(0, texture)
            RenderSystem.activeTexture(activeTexture)
            if (depthTest) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
            RenderSystem.depthMask(depthMask)
            GL11.glDepthRange(depthRange[0], depthRange[1])
            if (blend) RenderSystem.enableBlend() else RenderSystem.disableBlend()
            if (cull) RenderSystem.enableCull() else RenderSystem.disableCull()
            RenderSystem.stencilFunc(stencilFunction, stencilReference, stencilValueMask)
            RenderSystem.stencilOp(stencilFail, stencilDepthFail, stencilPass)
            if (stencil) GL11.glEnable(GL11.GL_STENCIL_TEST) else GL11.glDisable(GL11.GL_STENCIL_TEST)
        }
    }

    private fun renderMaskToTarget() {
        // 整屏热成像与镜内热成像是同一份遮罩：先画的那一遍负责生成，后面直接复用
        if (maskDrawn) return
        maskDrawn = true
        val mc = Minecraft.getInstance()
        val main = mc.mainRenderTarget
        val mask = target ?: TextureTarget(main.width, main.height, false, Minecraft.ON_OSX).also { target = it }
        if (mask.width != main.width || mask.height != main.height) mask.resize(main.width, main.height, Minecraft.ON_OSX)
        mask.setClearColor(0f, 0f, 0f, 0f)
        mask.clear(Minecraft.ON_OSX)
        val custom = maskShader ?: return
        val projection = worldProjection
        val modelView = worldModelView
        val worldState = ShaderCompat.getPostProcessWorldState()
        val depth = worldState?.second ?: if (hasWorldDepth) worldDepth?.depthTextureId ?: 0 else 0
        // World heat is occluded by hands/items; arm heat uses only the final hand/item depth.
        val handDepthTex = if (fullScreen && hasHandDepth) handDepth?.depthTextureId ?: 0 else 0
        val savedSampler = RenderSystem.getShaderTexture(1)
        val savedHandSampler = RenderSystem.getShaderTexture(2)
        try {
            if (depth > 0 && projection != null && modelView != null) {
                RenderSystem.setProjectionMatrix(worldState?.first ?: projection, RenderSystem.getVertexSorting())
                RenderSystem.getModelViewStack().setIdentity()
                RenderSystem.getModelViewStack().mulPoseMatrix(modelView)
                RenderSystem.applyModelViewMatrix()
                GL11.glDepthRange(0.0, 1.0)
                val iterator = active.values.iterator()
                while (iterator.hasNext()) {
                    val batch = iterator.next()
                    iterator.remove()
                    drawMaskBatch(batch, mask, custom, depth, handDepthTex)
                }
            }
            // Do not require a world heat source: empty scenes must still show hot arms.
            if (handDepthTex > 0) {
                val iterator = activeHands.iterator()
                while (iterator.hasNext()) {
                    val hand = iterator.next()
                    iterator.remove()
                    RenderSystem.setProjectionMatrix(hand.projection, RenderSystem.getVertexSorting())
                    RenderSystem.getModelViewStack().setIdentity()
                    RenderSystem.getModelViewStack().mulPoseMatrix(hand.modelView)
                    RenderSystem.applyModelViewMatrix()
                    GL11.glDepthRange(hand.depthNear, hand.depthFar)
                    drawMaskBatch(hand.batch, mask, custom, handDepthTex, handDepthTex, selfHand = true, stencil = hand.stencil)
                }
            }
        } finally {
            discardGeometry()
            RenderSystem.setShaderTexture(1, savedSampler)
            RenderSystem.setShaderTexture(2, savedHandSampler)
        }
    }

    private fun drawMaskBatch(
        batch: Batch, mask: TextureTarget, custom: ShaderInstance, depth: Int, handDepthTex: Int,
        selfHand: Boolean = false, stencil: HandStencil? = null,
    ) {
        val vertices = batch.builder.end()
        try {
            // Original skin alpha cuts out transparent sleeve pixels; guns never enter this batch.
            batch.outline.setupRenderState()
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, stencil?.target?.frameBufferId ?: mask.frameBufferId)
            if (stencil != null) {
                // Borrow only the saved clipping attachment; append colour to the shared heat mask.
                GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, mask.colorTextureId, 0)
            }
            RenderSystem.viewport(0, 0, mask.width, mask.height)
            RenderSystem.setShader { custom }
            RenderSystem.setShaderTexture(1, depth)
            RenderSystem.setShaderTexture(2, handDepthTex)
            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.disableBlend()
            if (stencil != null) {
                GL11.glEnable(GL11.GL_STENCIL_TEST)
                RenderSystem.stencilFunc(stencil.function, stencil.reference, stencil.valueMask)
                RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
            } else GL11.glDisable(GL11.GL_STENCIL_TEST)
            custom.safeGetUniform("DepthTexelSize").set(1f / mask.width, 1f / mask.height)
            custom.safeGetUniform("HandDepthEnabled").set(if (handDepthTex > 0) 1f else 0f)
            custom.safeGetUniform("SelfHandPass").set(if (selfHand) 1f else 0f)
            BufferUploader.drawWithShader(vertices)
        } finally {
            if (stencil != null) {
                GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, stencil.target.frameBufferId)
                GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0,
                    GL11.GL_TEXTURE_2D, stencil.target.colorTextureId, 0)
            }
            batch.outline.clearRenderState()
        }
    }

    fun maskTexture(): Int = target?.colorTextureId ?: 0

    internal fun applyColors(shader: ShaderInstance, backgroundColor: Int?, entityColor: Int?) {
        fun setColor(name: String, color: Int?) {
            val rgb = color ?: 0
            shader.safeGetUniform(name).set(
                ((rgb ushr 16) and 255) / 255f, ((rgb ushr 8) and 255) / 255f, (rgb and 255) / 255f,
                if (color == null) 0f else 1f
            )
        }
        setColor("ThermalBackgroundColor", backgroundColor)
        setColor("ThermalEntityColor", entityColor)
    }

    /**
     * 护目镜/载具热成像的全屏面：黑白通道，没有镜内的冷热配色与描边。
     *
     * 与 [beginSurface] 共用同一份场景截图（Sampler0）与热源遮罩（Sampler1），
     * 具体风格（像素块大小、噪点强度、暗角）写在 `rendertype_thermal_vision` 的默认 uniform 里。
     */
    private fun beginVisionSurface(): Boolean {
        val custom = visionShader ?: return false
        if (!ScopeLensShader.captureScreen()) return false
        RenderSystem.setShader { custom }
        RenderSystem.setShaderTexture(1, maskTexture())
        val main = Minecraft.getInstance().mainRenderTarget
        custom.safeGetUniform("ThermalTexelSize").set(1f / main.width, 1f / main.height)
        // Real-time sensor grain: bounded integer seed, independent of FPS and world time.
        custom.safeGetUniform("NoiseFrame").set(((System.nanoTime() / 33_333_333L) and 0xFFFFFL).toInt())
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        return true
    }

    fun beginSurface(
        backgroundColor: Int? = null,
        entityColor: Int? = null,
        magnification: Float = 1f,
    ): Boolean {
        val custom = surfaceShader ?: return false
        if (!ScopeLensShader.captureScreen()) return false
        RenderSystem.setShader { custom }
        RenderSystem.setShaderTexture(1, maskTexture())
        val main = Minecraft.getInstance().mainRenderTarget
        custom.safeGetUniform("ThermalTexelSize").set(1f / main.width, 1f / main.height)
        custom.safeGetUniform("ThermalMagnification").set(magnification.coerceAtLeast(0.0001f))
        applyColors(custom, backgroundColor, entityColor)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        return true
    }

    fun drawSurface(
        backgroundColor: Int? = null,
        entityColor: Int? = null,
        magnification: Float = 1f,
    ) {
        if (!beginSurface(backgroundColor, entityColor, magnification)) return
        val builder = Tesselator.getInstance().builder
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        builder.vertex(-1.0, -1.0, 0.0).uv(0f, 0f).endVertex()
        builder.vertex(1.0, -1.0, 0.0).uv(1f, 0f).endVertex()
        builder.vertex(1.0, 1.0, 0.0).uv(1f, 1f).endVertex()
        builder.vertex(-1.0, 1.0, 0.0).uv(0f, 1f).endVertex()
        BufferUploader.drawWithShader(builder.end())
    }

    private fun discardGeometry() {
        active.values.forEach { it.builder.end().release() }
        active.clear()
        activeHands.forEach { it.batch.builder.end().release() }
        activeHands.clear()
        handBuildersUsed = 0
        handStencilTargetsUsed = 0
    }

    fun close() {
        discardGeometry()
        target?.destroyBuffers()
        worldDepth?.destroyBuffers()
        handDepth?.destroyBuffers()
        handStencilTargets.forEach { it.destroyBuffers() }
        handStencilTargets.clear()
        target = null
        worldDepth = null
        handDepth = null
        hasWorldDepth = false
        hasHandDepth = false
        enabled = false
    }
}
