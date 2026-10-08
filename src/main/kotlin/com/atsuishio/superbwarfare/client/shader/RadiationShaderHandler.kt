package com.atsuishio.superbwarfare.client.shader

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.PostChain
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
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
        private var activeLevel = 0
        private var lastWidth = 0
        private var lastHeight = 0

        @SubscribeEvent
        fun onRegisterReloadListeners(event: RegisterClientReloadListenersEvent) {
            event.registerReloadListener(listener)
        }

        @JvmStatic
        fun setLevel(level: Int) {
            val newLevel = level.coerceIn(0, 20)
            if (activeLevel != newLevel) {
                activeLevel = newLevel
                if (newLevel == 0) cleanup()
            }
        }

        @JvmStatic
        fun getStrength(): Float {
            if (activeLevel <= 0) return 0.0f
            val normalized = (activeLevel - 1) / 19.0f
            return 0.12f + 0.88f * normalized
        }

        @JvmStatic
        fun render(event: RenderLevelStageEvent) {
            if (event.stage !== RenderLevelStageEvent.Stage.AFTER_LEVEL) return

            val mc = Minecraft.getInstance()
            if (activeLevel <= 0 || mc.player == null || mc.level == null ||
                mc.options.cameraType != net.minecraft.client.CameraType.FIRST_PERSON ||
                mc.gameRenderer.currentEffect() != null || ThermalShaderHandler.isActive()
            ) {
                return
            }

            RenderSystem.setShaderGameTime(0, event.partialTick)
            if (!ensureChain(mc)) return
            try {
                radiationChain?.process(event.partialTick)
            } catch (_: Exception) {
                cleanup()
            }
            mc.mainRenderTarget.bindWrite(true)
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
