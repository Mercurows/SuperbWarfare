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
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterShadersEvent
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL30

/** Copies the geometry of rendered living entities; it never changes their normal world render. */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = Mod.MODID, value = [Dist.CLIENT])
object ScopeThermalRenderer {
    private const val GEOMETRY_BUFFER_SIZE = 4096

    private data class Batch(val outline: RenderType, val builder: BufferBuilder)

    // 1.21 的 BufferBuilder 只能构造一次、build 之后不可复用；可复用的是它底下的 ByteBufferBuilder。
    private val geometryBuffers = mutableMapOf<RenderType, ByteBufferBuilder>()
    private val active = linkedMapOf<RenderType, Batch>()
    private var enabled = false
    private var worldProjection: Matrix4f? = null
    private var worldModelView: Matrix4f? = null
    private var target: TextureTarget? = null
    private var worldDepth: TextureTarget? = null
    private var hasWorldDepth = false
    private var maskShader: ShaderInstance? = null
    private var surfaceShader: ShaderInstance? = null

    // Kotlin for Forge registers the object instance, so this listener must not be static.
    @SubscribeEvent
    fun onRegisterShaders(event: RegisterShadersEvent) {
        event.registerShader(
            ShaderInstance(
                event.resourceProvider,
                ResourceLocation.fromNamespaceAndPath(Mod.MODID, "rendertype_scope_thermal_mask"), DefaultVertexFormat.POSITION_TEX_COLOR
            )
        ) {
            maskShader = it
        }
        event.registerShader(
            ShaderInstance(
                event.resourceProvider,
                ResourceLocation.fromNamespaceAndPath(Mod.MODID, "rendertype_scope_thermal"), DefaultVertexFormat.POSITION_TEX
            )
        ) {
            surfaceShader = it
        }
    }

    fun isAvailable(): Boolean = maskShader != null && surfaceShader != null

    @JvmStatic
    fun beginFrame() {
        discardGeometry()
        worldProjection = null
        worldModelView = null
        hasWorldDepth = false
        val mc = Minecraft.getInstance()
        val stack = mc.player?.mainHandItem
        enabled = isAvailable() && mc.options.cameraType.isFirstPerson && stack?.item is GunItem &&
                ClientEventHandler.zoomTime >= BedrockAttachmentModel.DIVISION_MIN_ZOOM &&
                GunData.from(stack).let { gun ->
                    gun.attachment.scopeStack().lastOrNull()?.definition?.scopeInfo
                        ?.mode(gun.attachment.scopeMode(AttachmentType.SCOPE))?.thermal == true
                }
    }

    /** Vanilla clears world depth before drawing hands, so preserve it before that clear. */
    @JvmStatic
    fun captureWorldDepth() {
        if (!enabled || ShaderCompat.isShaderPackActive()) return
        val main = Minecraft.getInstance().mainRenderTarget
        val drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val textureUnit = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
        RenderSystem.activeTexture(GL13.GL_TEXTURE0)
        val texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        try {
            if (worldDepth?.isStencilEnabled != main.isStencilEnabled) {
                worldDepth?.destroyBuffers()
                worldDepth = null
            }
            val copy = worldDepth ?: TextureTarget(main.width, main.height, true, Minecraft.ON_OSX).also {
                if (main.isStencilEnabled) it.enableStencil()
                worldDepth = it
            }
            if (copy.width != main.width || copy.height != main.height) copy.resize(
                main.width,
                main.height,
                Minecraft.ON_OSX
            )
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId)
            GlStateManager._glBlitFrameBuffer(
                0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST
            )
            hasWorldDepth = true
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo)
            GlStateManager._bindTexture(texture)
            RenderSystem.activeTexture(textureUnit)
        }
    }

    @JvmStatic
    fun capture(entity: Entity, source: MultiBufferSource): MultiBufferSource {
        if (!enabled || entity !is LivingEntity || !entity.isAlive || entity.isInvisible ||
            entity === Minecraft.getInstance().cameraEntity || ShaderCompat.isRenderingShadowPass()
        ) return source
        return MultiBufferSource { type ->
            val original = source.getBuffer(type)
            val outline = type.outline().orElse(null)
            if (outline == null || type.format() != DefaultVertexFormat.NEW_ENTITY) original else {
                if (worldProjection == null) {
                    worldProjection = Matrix4f(RenderSystem.getProjectionMatrix())
                    worldModelView = Matrix4f(RenderSystem.getModelViewMatrix())
                }
                val batch = active.getOrPut(type) {
                    val buffer = geometryBuffers.getOrPut(type) { ByteBufferBuilder(GEOMETRY_BUFFER_SIZE) }
                    Batch(outline, BufferBuilder(buffer, type.mode(), DefaultVertexFormat.POSITION_TEX_COLOR))
                }
                VertexMultiConsumer.create(original, MaskConsumer(batch.builder))
            }
        }
    }

    /** 1.21 的 VertexConsumer 以 addVertex 开始一个顶点，颜色/UV 随后写入。 */
    private class MaskConsumer(private val delegate: VertexConsumer) : VertexConsumer {
        override fun addVertex(x: Float, y: Float, z: Float): VertexConsumer {
            delegate.addVertex(x, y, z).setColor(255, 255, 255, 255)
            return this
        }

        override fun addVertex(
            x: Float, y: Float, z: Float, color: Int, u: Float, v: Float,
            packedOverlay: Int, packedLight: Int, normalX: Float, normalY: Float, normalZ: Float
        ) {
            delegate.addVertex(x, y, z).setColor(255, 255, 255, 255).setUv(u, v)
        }

        override fun setColor(red: Int, green: Int, blue: Int, alpha: Int): VertexConsumer = this
        override fun setColor(color: Int): VertexConsumer = this

        override fun setUv(u: Float, v: Float): VertexConsumer {
            delegate.setUv(u, v)
            return this
        }

        override fun setUv1(u: Int, v: Int): VertexConsumer = this
        override fun setUv2(u: Int, v: Int): VertexConsumer = this
        override fun setNormal(normalX: Float, normalY: Float, normalZ: Float): VertexConsumer = this
    }

    /** Runs after Iris's composite, when custom core shaders can write colour again. */
    fun renderMask() {
        val mc = Minecraft.getInstance()
        val main = mc.mainRenderTarget
        val mask = target ?: TextureTarget(main.width, main.height, false, Minecraft.ON_OSX).also { target = it }
        if (mask.width != main.width || mask.height != main.height) mask.resize(
            main.width,
            main.height,
            Minecraft.ON_OSX
        )
        mask.setClearColor(0f, 0f, 0f, 0f)
        mask.clear(Minecraft.ON_OSX)
        val custom = maskShader ?: return
        val projection = worldProjection ?: return
        val modelView = worldModelView ?: return
        val worldState = ShaderCompat.getPostProcessWorldState()
        val depth = worldState?.second ?: if (hasWorldDepth) worldDepth?.getDepthTextureId() ?: 0 else 0
        if (depth <= 0) return
        RenderSystem.setProjectionMatrix(worldState?.first ?: projection, RenderSystem.getVertexSorting())
        val stack = RenderSystem.getModelViewStack()
        stack.pushMatrix()
        try {
            stack.identity()
            stack.mul(modelView)
            RenderSystem.applyModelViewMatrix()
            val savedSampler = RenderSystem.getShaderTexture(1)
            try {
                for (batch in active.values) {
                    val vertices = batch.builder.build() ?: continue
                    try {
                        // The outline type supplies the original entity texture, including alpha cutouts.
                        batch.outline.setupRenderState()
                        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, mask.frameBufferId)
                        RenderSystem.viewport(0, 0, main.width, main.height)
                        RenderSystem.setShader { custom }
                        RenderSystem.setShaderTexture(1, depth)
                        RenderSystem.disableDepthTest()
                        RenderSystem.depthMask(false)
                        RenderSystem.disableBlend()
                        GL11.glDisable(GL11.GL_STENCIL_TEST)
                        custom.safeGetUniform("DepthTexelSize").set(1f / main.width, 1f / main.height)
                        BufferUploader.drawWithShader(vertices)
                    } finally {
                        batch.outline.clearRenderState()
                        vertices.close()
                    }
                }
            } finally {
                active.clear()
                RenderSystem.setShaderTexture(1, savedSampler)
            }
        } finally {
            stack.popMatrix()
            RenderSystem.applyModelViewMatrix()
        }
    }

    fun maskTexture(): Int = target?.getColorTextureId() ?: 0

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

    fun beginSurface(backgroundColor: Int? = null, entityColor: Int? = null): Boolean {
        val custom = surfaceShader ?: return false
        if (!ScopeLensShader.captureScreen()) return false
        RenderSystem.setShader { custom }
        RenderSystem.setShaderTexture(1, maskTexture())
        val main = Minecraft.getInstance().mainRenderTarget
        custom.safeGetUniform("ThermalTexelSize").set(1f / main.width, 1f / main.height)
        applyColors(custom, backgroundColor, entityColor)
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.disableDepthTest()
        RenderSystem.depthMask(false)
        return true
    }

    fun drawSurface(backgroundColor: Int? = null, entityColor: Int? = null) {
        if (!beginSurface(backgroundColor, entityColor)) return
        val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX)
        builder.addVertex(-1f, -1f, 0f).setUv(0f, 0f)
        builder.addVertex(1f, -1f, 0f).setUv(1f, 0f)
        builder.addVertex(1f, 1f, 0f).setUv(1f, 1f)
        builder.addVertex(-1f, 1f, 0f).setUv(0f, 1f)
        BufferUploader.drawWithShader(builder.buildOrThrow())
    }

    private fun discardGeometry() {
        active.values.forEach { it.builder.build()?.close() }
        active.clear()
    }

    fun close() {
        discardGeometry()
        geometryBuffers.values.forEach { it.close() }
        geometryBuffers.clear()
        target?.destroyBuffers()
        worldDepth?.destroyBuffers()
        target = null
        worldDepth = null
        enabled = false
    }
}
