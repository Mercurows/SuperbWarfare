package com.atsuishio.superbwarfare.client.overlay

import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.init.ModSounds
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance
import net.minecraft.client.resources.sounds.SoundInstance
import net.minecraft.sounds.SoundSource
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f
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
        effect.apply(visual, audio)
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
        refreshSoundVolumes()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun render(event: RenderGuiEvent.Post) {
        render(event.guiGraphics, event.partialTick)
    }

    private fun render(graphics: GuiGraphics, partialTick: Float) {
        if (client.player?.isAlive != true || client.level !== world) return
        val alpha = (effect.alpha(if (client.isPaused) 0f else partialTick) * 255).toInt().coerceIn(0, 255)
        if (alpha == 0) return
        val rgb = if (DisplayConfig.FLASHBANG_DARK_MODE.get()) 0 else 0xFFFFFF
        // Finish queued HUD/item draws first. guiOverlay explicitly disables depth testing,
        // unlike the default gui fill, so raised item icons cannot poke through the mask.
        graphics.flush()
        graphics.pose().pushPose()
        try {
            graphics.pose().setIdentity()
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
            graphics.fill(RenderType.guiOverlay(), 0, 0, client.window.guiScaledWidth, client.window.guiScaledHeight,
                (alpha shl 24) or rgb)
            graphics.flush()
        } finally {
            graphics.pose().popPose()
        }
    }

    /** F1 skips the GUI pass, but must not let a flashed player see through the effect. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun renderHiddenGui(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_LEVEL || !client.options.hideGui || client.screen != null
            || effect.visualTicks <= 0) return
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
            render(graphics, event.partialTick)
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
    private var fadeTicks = 1
    private var opacity = 0f
    private var audioDuration = 1
    private var audioStrength = 0f

    fun apply(visual: Float, audio: Float) {
        if (!visual.isFinite() || !audio.isFinite()) return
        val strength = visual.coerceIn(0f, 1f)
        // Remap positive exposure to 50–100% opacity; recovery can still fade all the way out.
        opacity = max(alpha(0f), if (strength > 0f) 0.5f + 0.5f * strength else 0f)
        fadeTicks = max(fadeTicks.coerceAtMost(visualTicks), (100 * strength).toInt().coerceAtLeast(1))
        visualTicks = max(visualTicks, fadeTicks + (20 * strength).toInt())
        audioStrength = max(ringVolume(), audio.coerceIn(0f, 1f))
        audioTicks = max(audioTicks, (160 * audio.coerceIn(0f, 1f)).toInt())
        audioDuration = audioTicks.coerceAtLeast(1)
    }

    fun tick() {
        if (visualTicks > 0) visualTicks--
        if (audioTicks > 0) audioTicks--
    }

    fun clear() {
        visualTicks = 0
        audioTicks = 0
        opacity = 0f
        audioStrength = 0f
    }

    fun alpha(partialTick: Float): Float = opacity *
        ((visualTicks - partialTick).coerceAtLeast(0f) / fadeTicks).coerceAtMost(1f).pow(0.75f)

    fun ringVolume(): Float = audioStrength * audioTicks.toFloat() / audioDuration

    // Keep a small floor so loops keep playing and can recover instead of being stopped as silent.
    fun otherVolume(): Float = 1f - 0.92f * ringVolume()
}
