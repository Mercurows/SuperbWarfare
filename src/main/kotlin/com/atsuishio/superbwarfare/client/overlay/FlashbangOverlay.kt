package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.init.ModSounds
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL30
import kotlin.math.max
import kotlin.math.pow

/** Flashbang presentation, independent of HUD visibility and player damage. */
@Mod.EventBusSubscriber(value = [Dist.CLIENT])
object FlashbangOverlay {
    private val client get() = Minecraft.getInstance()
    private var world: ClientLevel? = null
    private val effect = FlashbangEffect()
    private var ringing: RingingSound? = null
    private var lastOtherVolume = 1f
    private var capturePending = false
    private var afterimage: TextureTarget? = null
    private var captureWidth = 0
    private var captureHeight = 0

    /** Applied at the sound engine, without changing saved category or master volume settings. */
    @JvmStatic
    fun soundVolumeMultiplier(sound: SoundInstance): Float =
        if (sound is RingingSound) 1f else effect.otherVolume()

    private fun refreshSoundVolumes() {
        val multiplier = effect.otherVolume()
        if (multiplier == lastOtherVolume) return
        lastOtherVolume = multiplier
        // Vanilla recalculates ALL active channels for any non-master category update.
        client.soundManager.updateSourceVolume(SoundSource.PLAYERS, client.options.getSoundSourceVolume(SoundSource.PLAYERS))
    }

    fun apply(visual: Float, audio: Float) {
        val player = client.player ?: return
        if (!player.isAlive || player.isSpectator || !visual.isFinite() || !audio.isFinite()) return
        if (world !== client.level) clear()
        world = client.level
        if (effect.apply(visual, audio)) capturePending = true
        refreshSoundVolumes()
        if (effect.audioTicks > 0 && ringing == null) {
            ringing = RingingSound().also { client.soundManager.play(it) }
        }
    }

    fun tick() {
        if (client.level !== world || client.player?.isAlive != true || client.player?.isSpectator == true) {
            clear()
            return
        }
        if (client.isPaused) return
        effect.tick()
        if (effect.afterimageTicks == 0) discardAfterimage()
        refreshSoundVolumes()
        if (effect.audioTicks == 0) {
            ringing?.let { client.soundManager.stop(it) }
            ringing = null
        }
    }

    fun clear() {
        ringing?.let { client.soundManager.stop(it) }
        ringing = null
        world = null
        effect.clear()
        discardAfterimage()
        refreshSoundVolumes()
    }

    private fun discardAfterimage() {
        capturePending = false
        val target = afterimage ?: return
        afterimage = null
        val release = {
            // destroyBuffers unbinds both the framebuffer and current texture, including during resize.
            val readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
            val drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
            val texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
            val deletedFbo = target.frameBufferId
            val deletedTexture = target.colorTextureId
            try {
                target.destroyBuffers()
            } finally {
                GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,
                    if (readFbo == deletedFbo) client.mainRenderTarget.frameBufferId else readFbo)
                GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,
                    if (drawFbo == deletedFbo) client.mainRenderTarget.frameBufferId else drawFbo)
                GlStateManager._bindTexture(if (texture == deletedTexture) 0 else texture)
            }
        }
        if (RenderSystem.isOnRenderThread()) release()
        else RenderSystem.recordRenderCall { release() }
    }

    /** Copy once on the GPU, after world/hands and before any flash or HUD is painted. */
    private fun captureAfterimage() {
        capturePending = false
        val main = client.mainRenderTarget
        val readFbo = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val drawFbo = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)
        val viewport = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport)
        val activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)
        RenderSystem.activeTexture(GL13.GL_TEXTURE0)
        val texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)
        val depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
        val clearColor = BufferUtils.createFloatBuffer(4)
        GL11.glGetFloatv(GL11.GL_COLOR_CLEAR_VALUE, clearColor)
        val scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)
        val scissorBox = BufferUtils.createIntBuffer(4)
        GL11.glGetIntegerv(GL11.GL_SCISSOR_BOX, scissorBox)
        try {
            RenderSystem.disableScissor()
            val width = (main.width / 2).coerceAtLeast(1)
            val height = (main.height / 2).coerceAtLeast(1)
            val target = afterimage ?: TextureTarget(width, height, false, Minecraft.ON_OSX).also { afterimage = it }
            if (target.width != width || target.height != height) target.resize(width, height, Minecraft.ON_OSX)
            target.setFilterMode(GL11.GL_LINEAR)
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, target.frameBufferId)
            GlStateManager._glBlitFrameBuffer(0, 0, main.width, main.height, 0, 0, width, height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_LINEAR)
            captureWidth = main.width
            captureHeight = main.height
        } catch (e: RuntimeException) {
            discardAfterimage()
            com.atsuishio.superbwarfare.Mod.LOGGER.warn("Could not capture flashbang afterimage", e)
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFbo)
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFbo)
            GlStateManager._bindTexture(texture)
            RenderSystem.activeTexture(activeTexture)
            RenderSystem.viewport(viewport[0], viewport[1], viewport[2], viewport[3])
            RenderSystem.clearColor(clearColor[0], clearColor[1], clearColor[2], clearColor[3])
            if (depthTest) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
            if (scissor) RenderSystem.enableScissor(scissorBox[0], scissorBox[1], scissorBox[2], scissorBox[3])
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun render(event: RenderGuiEvent.Post) {
        render(event.guiGraphics, event.partialTick)
    }

    private fun render(graphics: GuiGraphics, partialTick: Float) {
        if (client.player?.isAlive != true || client.level !== world) return
        val frameTime = if (client.isPaused) 0f else partialTick
        val alpha = (effect.alpha(frameTime) * 255).toInt().coerceIn(0, 255)
        val ghostAlpha = effect.afterimageAlpha(frameTime)
        if (alpha == 0 && (afterimage == null || ghostAlpha <= 0f)) return
        val rgb = if (DisplayConfig.FLASHBANG_DARK_MODE.get()) 0 else 0xFFFFFF
        // Finish queued HUD/item draws first. guiOverlay explicitly disables depth testing,
        // unlike the default gui fill, so raised item icons cannot poke through the mask.
        graphics.flush()
        graphics.pose().pushPose()
        try {
            graphics.pose().setIdentity()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            if (ghostAlpha > 0f) drawAfterimage(graphics, ghostAlpha)
            if (alpha > 0) graphics.fill(RenderType.guiOverlay(), 0, 0,
                client.window.guiScaledWidth, client.window.guiScaledHeight, (alpha shl 24) or rgb)
            graphics.flush()
        } finally {
            graphics.pose().popPose()
        }
    }

    private fun drawAfterimage(graphics: GuiGraphics, alpha: Float) {
        val target = afterimage ?: return
        val shader = RenderSystem.getShader()
        val texture = RenderSystem.getShaderTexture(0)
        val depthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST)
        val depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK)
        val blend = GL11.glIsEnabled(GL11.GL_BLEND)
        val srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB)
        val dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB)
        val srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA)
        val dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA)
        try {
            RenderSystem.disableDepthTest()
            RenderSystem.depthMask(false)
            RenderSystem.enableBlend()
            RenderSystem.defaultBlendFunc()
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader)
            RenderSystem.setShaderTexture(0, target.colorTextureId)
            val matrix = graphics.pose().last().pose()
            val width = client.window.guiScaledWidth.toFloat()
            val height = client.window.guiScaledHeight.toFloat()
            val buffer = Tesselator.getInstance().builder
            buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
            // Framebuffer textures have a bottom-left origin; GUI coordinates start at the top-left.
            buffer.vertex(matrix, 0f, height, 0f).uv(0f, 0f).color(1f, 1f, 1f, alpha).endVertex()
            buffer.vertex(matrix, width, height, 0f).uv(1f, 0f).color(1f, 1f, 1f, alpha).endVertex()
            buffer.vertex(matrix, width, 0f, 0f).uv(1f, 1f).color(1f, 1f, 1f, alpha).endVertex()
            buffer.vertex(matrix, 0f, 0f, 0f).uv(0f, 1f).color(1f, 1f, 1f, alpha).endVertex()
            BufferUploader.drawWithShader(buffer.end())
        } finally {
            RenderSystem.setShaderTexture(0, texture)
            RenderSystem.setShader { shader }
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha)
            if (blend) RenderSystem.enableBlend() else RenderSystem.disableBlend()
            RenderSystem.depthMask(depthMask)
            if (depthTest) RenderSystem.enableDepthTest() else RenderSystem.disableDepthTest()
        }
    }

    /** Called at the end of world rendering; F1 still needs its own GUI projection for the mask. */
    @JvmStatic
    fun finishWorldFrame(partialTick: Float) {
        if (client.player?.isAlive != true || client.level !== world) return
        if (capturePending && effect.visualTicks > 0) captureAfterimage()
        else if (capturePending) discardAfterimage() // Never latch a new scene after visual recovery.
        else if (afterimage != null && (captureWidth != client.mainRenderTarget.width
                    || captureHeight != client.mainRenderTarget.height)) discardAfterimage()
        if (!client.options.hideGui || client.screen != null
            || (effect.visualTicks <= 0 && effect.afterimageTicks <= 0)) return
        val projection = Matrix4f(RenderSystem.getProjectionMatrix())
        val sorting = RenderSystem.getVertexSorting()
        val modelView = RenderSystem.getModelViewStack()
        modelView.pushPose()
        try {
            modelView.setIdentity()
            modelView.translate(0.0, 0.0, -11000.0)
            RenderSystem.applyModelViewMatrix()
            RenderSystem.setProjectionMatrix(Matrix4f().setOrtho(
                0f, client.window.guiScaledWidth.toFloat(), client.window.guiScaledHeight.toFloat(), 0f, 1000f, 21000f
            ), VertexSorting.ORTHOGRAPHIC_Z)
            val graphics = GuiGraphics(client, client.renderBuffers().bufferSource())
            render(graphics, partialTick)
            graphics.flush()
        } finally {
            modelView.popPose()
            RenderSystem.applyModelViewMatrix()
            RenderSystem.setProjectionMatrix(projection, sorting)
        }
    }

    private class RingingSound : AbstractTickableSoundInstance(
        ModSounds.FLASHBANG_RINGING.get(), SoundSource.PLAYERS, SoundInstance.createUnseededRandom()
    ) {
        init {
            looping = true
            relative = true
            attenuation = SoundInstance.Attenuation.NONE
            volume = effect.ringVolume() * 0.65f
        }

        override fun tick() {
            if (effect.audioTicks <= 0 || client.level !== world || client.player?.isAlive != true) {
                stop()
            } else {
                volume = effect.ringVolume() * 0.65f
            }
        }
    }
}

/** Pure timing state; kept beside its presentation so recovery can be checked without a game window. */
internal class FlashbangEffect {
    var visualTicks = 0
        private set
    var audioTicks = 0
        private set
    var afterimageTicks = 0
        private set
    private var fadeTicks = 1
    private var opacity = 0f
    private var audioDuration = 1
    private var audioStrength = 0f
    private var afterimageOpacity = 0f

    /** Returns true only for the first visual hit of a flash/recovery episode. */
    fun apply(visual: Float, audio: Float): Boolean {
        if (!visual.isFinite() || !audio.isFinite()) return false
        val strength = visual.coerceIn(0f, 1f)
        val capture = strength > 0f && visualTicks == 0 && afterimageTicks == 0
        if (strength > 0f) {
            afterimageTicks = 20
            afterimageOpacity = max(afterimageOpacity, (0.5f + 0.5f * strength) * 0.18f)
            // Remap positive exposure to 50–100% opacity; audio-only hits leave visual recovery untouched.
            opacity = max(alpha(0f), 0.5f + 0.5f * strength)
            fadeTicks = max(fadeTicks.coerceAtMost(visualTicks), (100 * strength).toInt().coerceAtLeast(1))
            visualTicks = max(visualTicks, fadeTicks + (20 * strength).toInt())
        }
        audioStrength = max(ringVolume(), audio.coerceIn(0f, 1f))
        audioTicks = max(audioTicks, (160 * audio.coerceIn(0f, 1f)).toInt())
        audioDuration = audioTicks.coerceAtLeast(1)
        return capture
    }

    fun tick() {
        if (visualTicks > 0) visualTicks--
        else if (afterimageTicks > 0) afterimageTicks--
        if (afterimageTicks == 0) afterimageOpacity = 0f
        if (audioTicks > 0) audioTicks--
    }

    fun clear() {
        visualTicks = 0
        audioTicks = 0
        afterimageTicks = 0
        opacity = 0f
        audioStrength = 0f
        afterimageOpacity = 0f
    }

    fun alpha(partialTick: Float): Float = opacity *
        ((visualTicks - partialTick).coerceAtLeast(0f) / fadeTicks).coerceAtMost(1f).pow(0.75f)

    fun ringVolume(): Float = audioStrength * audioTicks.toFloat() / audioDuration

    // Keep a small floor so loops keep playing and can recover instead of being stopped as silent.
    fun otherVolume(): Float = 1f - 0.92f * ringVolume()

    fun afterimageAlpha(partialTick: Float): Float {
        val remaining = if (visualTicks > 0) 1f else ((afterimageTicks - partialTick) / 20f).coerceIn(0f, 1f)
        val flash = alpha(partialTick)
        // Reveal the already-frozen frame through the white/black mask, then fade it over the live scene.
        return if (visualTicks > 0 && afterimageTicks > 0) flash + afterimageOpacity * (1f - flash)
        else afterimageOpacity * remaining
    }
}
