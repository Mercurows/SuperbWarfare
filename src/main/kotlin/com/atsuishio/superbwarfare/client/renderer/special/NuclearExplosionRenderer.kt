package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.lighting.ProjectileLightHelper
import com.atsuishio.superbwarfare.client.particle.CustomCloudOption
import com.atsuishio.superbwarfare.client.particle.NuclearSmokeParticle
import com.atsuishio.superbwarfare.compat.shader.ShaderCompat
import com.atsuishio.superbwarfare.entity.projectile.NuclearBombEntity.NuclearExplosionParameters
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferUploader
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.Tesselator
import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.logging.LogUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.core.BlockPos
import net.minecraft.util.RandomSource
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.RegisterShadersEvent
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.*
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/** One bounded ray-marched volume per detonation, independent of particle-count settings. */
@OnlyIn(Dist.CLIENT)
object NuclearExplosionRenderer {
    private const val MAX_EFFECTS = 4

    /**
     * 底部烟尘水平范围倍率：只放大贴地那层（尘front 粒子 + 持续堆积带）的半径，高度不变。
     * 必须与 `nuclear_explosion.fsh` 里的 `DUST_FOOT_SCALE` 保持一致，否则 shader 体积与粒子层会错位。
     */
    private const val DUST_FOOT_SCALE = 1.4

    private data class Burst(
        val position: Vec3,
        val start: Long,
        val seed: Int,
        val parameters: NuclearExplosionParameters
    ) {
        val random: RandomSource = RandomSource.create(seed.toLong())
        fun isAlive(gameTime: Long) = parameters.isAlive(gameTime - start)
        fun age(gameTime: Long, partialTick: Float = 0f) =
            parameters.ageSeconds(gameTime - start + partialTick)
    }

    private val logger = LogUtils.getLogger()
    private val bursts = mutableListOf<Burst>()
    private var effectLevel: ClientLevel? = null
    private var shader: ShaderInstance? = null
    private var compositeShader: ShaderInstance? = null
    private var scene: TextureTarget? = null
    private var volume: TextureTarget? = null
    private var failed = false

    fun registerShader(event: RegisterShadersEvent) {
        event.registerShader(
            ShaderInstance(
                event.resourceProvider,
                loc("nuclear_explosion"),
                DefaultVertexFormat.POSITION
            )
        ) {
            shader = it
            failed = false
            releaseTarget()
        }
        event.registerShader(
            ShaderInstance(
                event.resourceProvider,
                loc("nuclear_composite"),
                DefaultVertexFormat.POSITION
            )
        ) {
            compositeShader = it
        }
    }

    fun spawn(
        dimension: String,
        position: Vec3,
        seed: Int,
        parameters: NuclearExplosionParameters = NuclearExplosionParameters()
    ) {
        val level = Minecraft.getInstance().level ?: return
        if (level.dimension().location().toString() != dimension) return
        if (effectLevel !== level) clear()
        effectLevel = level
        if (bursts.size >= MAX_EFFECTS) bursts.removeAt(0)
        val effect = parameters.sanitized()
        bursts.add(Burst(position, level.gameTime, seed, effect))
        ProjectileLightHelper.emitExplosionFlashDirect(level, position, 28f * effect.radiusScale)
    }

    @JvmStatic
    fun clear() {
        bursts.clear()
        effectLevel = null
        releaseTarget()
    }

    private fun releaseTarget() {
        scene?.destroyBuffers()
        scene = null
        volume?.destroyBuffers()
        volume = null
    }

    /** Supplemental puffs are emitted once per game tick, never once per rendered frame. */
    fun tick() {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        if (mc.isPaused || effectLevel !== level) return
        bursts.removeAll { !it.isAlive(level.gameTime) }
        val camera = mc.gameRenderer.mainCamera.position
        for (burst in bursts) {
            val tick = level.gameTime - burst.start
            val parameters = burst.parameters
            val horizontalScale = parameters.radiusScale.toDouble()
            val verticalScale = parameters.heightScale.toDouble()
            val particleScale = minOf(horizontalScale, verticalScale).toFloat()
            val motionScale = horizontalScale * parameters.timeScale
            val verticalMotionScale = verticalScale * parameters.timeScale
            val particleRange = 512.0 + parameters.radius * 5.0
            if (burst.position.distanceToSqr(camera) > particleRange * particleRange) continue
            val phaseTick = (tick * parameters.timeScale).toLong()
            val seconds = burst.age(level.gameTime).toDouble()
            val remainingTicks = (parameters.durationTicks - tick).toInt().coerceAtLeast(1)
            val random = burst.random
            val capRadius = 2.0 + 26.0 * (1.0 - exp(-seconds * 2.4)) +
                    32.0 * (1.0 - exp(-maxOf(seconds - 1.0, 0.0) / 8.0))
            // Small, sparse puffs follow the lowered dust front without a tall billboard fringe.
            if (phaseTick in 6..240 && tick % 6L == 0L) repeat(3) {
                val angle = random.nextDouble() * Math.PI * 2
                val radius = (5.0 + 51.0 * (1.0 - exp(-seconds / 0.65))) * horizontalScale * DUST_FOOT_SCALE
                val x = burst.position.x + cos(angle) * radius
                val z = burst.position.z + sin(angle) * radius
                val block = BlockPos.containing(x, burst.position.y, z)
                if (!level.hasChunkAt(block)) return@repeat
                val ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, block.x, block.z)
                if (kotlin.math.abs(ground - burst.position.y) > 24) return@repeat
                mc.particleEngine.add(
                    NuclearSmokeParticle(
                        level, x, ground + 0.3 * verticalScale, z,
                        cos(angle) * 0.18 * motionScale, (0.02 + random.nextDouble() * 0.03) * verticalMotionScale,
                        sin(angle) * 0.18 * motionScale, (0.9f + random.nextFloat() * 0.9f) * particleScale,
                        minOf(70 + random.nextInt(40), remainingTicks), true
                    )
                )
            }
            // The white pressure front crosses the scene in under a second, ahead of the short dust wave.
            if (phaseTick in 1..18) repeat(24) { index ->
                val angle = (index + random.nextDouble()) * Math.PI * 2.0 / 24.0
                val radius = (10.0 + 280.0 * (1.0 - exp(-seconds / 0.24))) * horizontalScale
                val x = burst.position.x + cos(angle) * radius
                val z = burst.position.z + sin(angle) * radius
                val block = BlockPos.containing(x, burst.position.y, z)
                if (!level.hasChunkAt(block)) return@repeat
                val ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, block.x, block.z)
                if (kotlin.math.abs(ground - burst.position.y) > 24) return@repeat
                level.addParticle(
                    CustomCloudOption(
                        0xFFFFFF, minOf(7, remainingTicks), 24f * particleScale,
                        0f, cooldown = false, light = true
                    ), true, x, ground + 1.6 * verticalScale, z,
                    cos(angle) * 8.0 * motionScale, 0.04 * verticalMotionScale, sin(angle) * 8.0 * motionScale
                )
            }
            // Persistent low billows cover uneven terrain around the volume's accumulating base.
            if (phaseTick in 24..820 && tick % 8L == 0L) repeat(2) {
                val angle = random.nextDouble() * Math.PI * 2
                val radialFraction = kotlin.math.sqrt(random.nextDouble())
                val radius = radialFraction * capRadius * 1.18 * horizontalScale * DUST_FOOT_SCALE
                val edgeProgress = ((radialFraction - 0.15) / 0.85).coerceIn(0.0, 1.0)
                val heightTaper = 1.0 - 0.82 * edgeProgress * edgeProgress * (3.0 - 2.0 * edgeProgress)
                val x = burst.position.x + cos(angle) * radius
                val z = burst.position.z + sin(angle) * radius
                val block = BlockPos.containing(x, burst.position.y, z)
                if (!level.hasChunkAt(block)) return@repeat
                val ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, block.x, block.z)
                if (kotlin.math.abs(ground - burst.position.y) > 24) return@repeat
                mc.particleEngine.add(
                    NuclearSmokeParticle(
                        level,
                        x,
                        ground + (0.3 + random.nextDouble() * 0.9 * heightTaper) * verticalScale,
                        z,
                        cos(angle) * 0.04 * motionScale,
                        (0.012 + random.nextDouble() * 0.024) * heightTaper * verticalMotionScale,
                        sin(angle) * 0.04 * motionScale,
                        (1.2f + random.nextFloat() * 0.9f) * heightTaper.toFloat() * particleScale,
                        minOf(120 + random.nextInt(60), remainingTicks),
                        true
                    )
                )
            }
            // A sparse fringe detaches from the crown, continuing the shader's rise and wind drift.
            if (parameters.renderMushroomCloud && phaseTick in 40..800 && tick % 6L == 0L) {
                val angle = random.nextDouble() * Math.PI * 2
                val radius = capRadius * 0.94 * horizontalScale
                val height = (5.0 + 95.0 * (1.0 - exp(-maxOf(seconds - 0.7, 0.0) / 8.0))) * verticalScale
                val point = burst.position.add(
                    seconds * 0.24 * horizontalScale + cos(angle) * radius,
                    height + random.nextDouble() * 10.0 * verticalScale,
                    seconds * 0.11 * horizontalScale + sin(angle) * radius
                )
                if (level.hasChunkAt(BlockPos.containing(point))) {
                    mc.particleEngine.add(
                        NuclearSmokeParticle(
                            level,
                            point.x,
                            point.y,
                            point.z,
                            (cos(angle) * 0.07 + 0.03) * motionScale,
                            (0.08 + random.nextDouble() * 0.08) * verticalMotionScale,
                            sin(angle) * 0.07 * motionScale,
                            (2f + random.nextFloat() * 3f) * particleScale,
                            minOf(90 + random.nextInt(50), remainingTicks),
                            false
                        )
                    )
                }
            }
        }
    }

    /** Called after the world composite, before scopes/thermal imaging and first-person hands. */
    @JvmStatic
    fun render(partialTick: Float) {
        val mc = Minecraft.getInstance()
        val level = mc.level
        if (effectLevel !== level || level == null) {
            if (effectLevel != null) clear()
            return
        }
        bursts.removeAll { !it.isAlive(level.gameTime) }
        if (bursts.isEmpty()) {
            if (scene != null) releaseTarget()
            return
        }
        val custom = shader ?: return
        val composite = compositeShader ?: return
        if (failed || ShaderCompat.isRenderingShadowPass()) return
        val view = ClientEventHandler.modelViewMatrix ?: return
        val shaderPack = ShaderCompat.isShaderPackActive()
        val worldState = if (shaderPack) ShaderCompat.getPostProcessWorldState() ?: return else null
        val projection = worldState?.first ?: ClientEventHandler.projectionMatrix ?: return
        val main = mc.mainRenderTarget
        if (main.width <= 0 || main.height <= 0 || main.depthTextureId <= 0) return
        val camera = mc.gameRenderer.mainCamera.position

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
        val textures = IntArray(4) {
            RenderSystem.activeTexture(GL13.GL_TEXTURE0 + it)
            GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        }
        val capabilities = GL.getCapabilities()
        val samplers = if (capabilities.OpenGL33 || capabilities.GL_ARB_sampler_objects)
            IntArray(4) { GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING, it) } else null
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
                copy.isStencilEnabled != main.isStencilEnabled
            ) {
                copy?.destroyBuffers()
                copy = TextureTarget(main.width, main.height, true, Minecraft.ON_OSX)
                scene = copy
                if (main.isStencilEnabled) copy.enableStencil()
            }
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId)
            GL30.glBlitFramebuffer(
                0, 0, main.width, main.height, 0, 0, main.width, main.height,
                GL11.GL_COLOR_BUFFER_BIT or GL11.GL_DEPTH_BUFFER_BIT, GL11.GL_NEAREST
            )
            // Bound GPU cost even at high display resolutions. The resolve rejects depth discontinuities.
            val scale = minOf(0.5, 960.0 / main.width, 540.0 / main.height)
            val volumeWidth = (main.width * scale).toInt().coerceAtLeast(1)
            val volumeHeight = (main.height * scale).toInt().coerceAtLeast(1)
            var lowResolution = volume
            if (lowResolution == null || lowResolution.width != volumeWidth || lowResolution.height != volumeHeight) {
                lowResolution?.destroyBuffers()
                lowResolution = TextureTarget(volumeWidth, volumeHeight, false, Minecraft.ON_OSX)
                lowResolution.setClearColor(0f, 0f, 0f, 0f)
                volume = lowResolution
            }
            lowResolution.clear(Minecraft.ON_OSX)
            lowResolution.bindWrite(true)
            samplers?.indices?.forEach { GL33.glBindSampler(it, 0) }
            custom.setSampler("SceneColor", copy.colorTextureId)
            custom.setSampler("SceneDepth", copy.depthTextureId)
            custom.setSampler("WorldDepth", worldState?.second ?: copy.depthTextureId)
            custom.safeGetUniform("InverseProjection").set(Matrix4f(projection).invert())
            custom.safeGetUniform("InverseView").set(Matrix4f(view).invert())
            custom.safeGetUniform("ShaderPackMode").set(if (shaderPack) 1 else 0)
            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.disableCull()
            RenderSystem.enableBlend()
            GL20.glBlendEquationSeparate(GL14.GL_FUNC_ADD, GL14.GL_FUNC_ADD)
            // The volume integrates premultiplied radiance and transmittance front to back.
            RenderSystem.blendFuncSeparate(
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA
            )
            RenderSystem.setShader { custom }
            for (burst in bursts.sortedByDescending { it.position.distanceToSqr(camera) }) {
                val relative = burst.position.subtract(camera)
                val age = burst.age(level.gameTime, partialTick)
                val parameters = burst.parameters
                val source = burst.position.add(0.0, 4.0 * parameters.heightScale, 0.0)
                val visible = age < 4f && (level.clip(
                    ClipContext(
                        camera, source,
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mc.cameraEntity
                    )
                ).let {
                    it.type == HitResult.Type.MISS || it.location.distanceToSqr(source) < 16.0
                })
                custom.safeGetUniform("ExplosionPosition")
                    .set(relative.x.toFloat(), relative.y.toFloat(), relative.z.toFloat())
                custom.safeGetUniform("Age").set(age)
                custom.safeGetUniform("FlowTime").set(age)
                custom.safeGetUniform("EffectScale")
                    .set(parameters.radiusScale, parameters.heightScale, parameters.radiusScale)
                custom.safeGetUniform("Seed").set((burst.seed and 65535).toFloat() / 65535f)
                custom.safeGetUniform("FlashVisibility").set(if (visible) 1f else 0f)
                custom.safeGetUniform("CloudVisibility").set(if (parameters.renderMushroomCloud) 1f else 0f)
                val builder = Tesselator.getInstance().builder
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
                builder.vertex(-1.0, -1.0, 0.0).endVertex()
                builder.vertex(1.0, -1.0, 0.0).endVertex()
                builder.vertex(1.0, 1.0, 0.0).endVertex()
                builder.vertex(-1.0, 1.0, 0.0).endVertex()
                BufferUploader.drawWithShader(builder.end())
            }
            main.bindWrite(true)
            composite.setSampler("VolumeColor", lowResolution.colorTextureId)
            composite.setSampler("SceneDepth", copy.depthTextureId)
            composite.setSampler("WorldDepth", worldState?.second ?: copy.depthTextureId)
            composite.setSampler("SceneColor", copy.colorTextureId)
            composite.safeGetUniform("InverseProjection").set(Matrix4f(projection).invert())
            composite.safeGetUniform("InverseView").set(Matrix4f(view).invert())
            composite.safeGetUniform("ShaderPackMode").set(if (shaderPack) 1 else 0)
            composite.safeGetUniform("HeatCount").set(bursts.size)
            bursts.forEachIndexed { index, burst ->
                val relative = burst.position.subtract(camera)
                composite.safeGetUniform("Heat$index").set(
                    relative.x.toFloat(), relative.y.toFloat(), relative.z.toFloat(),
                    burst.age(level.gameTime, partialTick)
                )
                composite.safeGetUniform("HeatScale$index")
                    .set(burst.parameters.radiusScale, burst.parameters.heightScale)
            }
            RenderSystem.disableBlend()
            RenderSystem.setShader { composite }
            val resolve = Tesselator.getInstance().builder
            resolve.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
            resolve.vertex(-1.0, -1.0, 0.0).endVertex()
            resolve.vertex(1.0, -1.0, 0.0).endVertex()
            resolve.vertex(1.0, 1.0, 0.0).endVertex()
            resolve.vertex(-1.0, 1.0, 0.0).endVertex()
            BufferUploader.drawWithShader(resolve.end())
        } catch (exception: Exception) {
            failed = true
            releaseTarget()
            logger.warn("Nuclear volume rendering disabled until resource reload", exception)
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo)
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo)
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
            RenderSystem.colorMask(
                colorMask[0].toInt() != 0, colorMask[1].toInt() != 0,
                colorMask[2].toInt() != 0, colorMask[3].toInt() != 0
            )
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
}
