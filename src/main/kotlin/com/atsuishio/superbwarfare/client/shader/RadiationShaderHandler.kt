package com.atsuishio.superbwarfare.client.shader

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.mobeffect.RadiationMobEffect
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.renderer.PostChain
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.client.event.RenderGuiEvent
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

class RadiationShaderHandler : ResourceManagerReloadListener {
    override fun onResourceManagerReload(resourceManager: ResourceManager) {
        cleanup()
    }

    @Mod.EventBusSubscriber(value = [Dist.CLIENT], bus = Mod.EventBusSubscriber.Bus.MOD)
    companion object {
        private val RADIATION_EFFECT = loc("shaders/post/radiation.json")
        private val listener = RadiationShaderHandler()
        private var radiationChain: PostChain? = null
        private var activeDose = 0f
        private var lastWidth = 0
        private var lastHeight = 0

        @SubscribeEvent
        fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
            event.registerReloadListener(listener)
        }

        @JvmStatic
        fun setDosage(dose: Float) {
            val newDose = dose.coerceAtLeast(0f)
            if (activeDose != newDose) {
                activeDose = newDose
                if (newDose <= 0f) cleanup()
            }
        }

        @JvmStatic
        fun getStrength(): Float {
            return RadiationMobEffect.getSaturation(activeDose)
        }

        @JvmStatic
        fun render(event: RenderGuiEvent.Post) {
            if (mc.options.hideGui && mc.screen == null) return

            process(event.partialTick)
        }

        @JvmStatic
        fun renderLevel(event: RenderLevelStageEvent) {
            if (event.stage !== RenderLevelStageEvent.Stage.AFTER_LEVEL) return
            if (!mc.options.hideGui || mc.screen != null) return

            process(event.partialTick)
        }

        private fun process(partialTick: Float) {
            if (activeDose <= 0f || localPlayer == null || clientLevel == null ||
                mc.options.cameraType != CameraType.FIRST_PERSON ||
                mc.gameRenderer.currentEffect() != null || ThermalShaderHandler.isActive() ||
                !keepsWorldVisible()
            ) {
                return
            }

            RenderSystem.setShaderGameTime(0, partialTick)
            if (!ensureChain(mc)) return
            try {
                radiationChain?.process(partialTick)
            } catch (_: Exception) {
                cleanup()
            }
            mc.mainRenderTarget.bindWrite(true)
        }

        private fun keepsWorldVisible(): Boolean {
            val screen = mc.screen ?: return true
            return screen is ChatScreen
        }

        private fun ensureChain(mc: Minecraft): Boolean {
            if (radiationChain == null) {
                try {
                    radiationChain = PostChain(
                        mc.textureManager,
                        mc.resourceManager,
                        mc.mainRenderTarget,
                        RADIATION_EFFECT
                    )
                    radiationChain!!.resize(mc.window.width, mc.window.height)
                    lastWidth = mc.window.width
                    lastHeight = mc.window.height
                } catch (e: Exception) {
                    e.printStackTrace()
                    cleanup()
                    return false
                }
            }

            if (lastWidth != mc.window.width || lastHeight != mc.window.height) {
                lastWidth = mc.window.width
                lastHeight = mc.window.height
                radiationChain!!.resize(lastWidth, lastHeight)
            }
            return true
        }

        private fun cleanup() {
            radiationChain?.close()
            radiationChain = null
            lastWidth = 0
            lastHeight = 0
        }
    }
}
