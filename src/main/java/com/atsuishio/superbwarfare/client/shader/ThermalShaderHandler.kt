package com.atsuishio.superbwarfare.client.shader

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.PostChain
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.neoforge.client.event.RenderLevelStageEvent

/**
 * Code based on YWZJ Team
 */
class ThermalShaderHandler : ResourceManagerReloadListener {
    override fun onResourceManagerReload(resourceManager: ResourceManager) {
        cleanup()
    }

    companion object {
        private val THERMAL_EFFECT = loc("shaders/post/thermal.json")
        private var isActive = false
        private var thermalChain: PostChain? = null
        private var lastWidth = 0
        private var lastHeight = 0
        private var seeThroughWalls = false

        fun setSeeThroughWalls(seeThrough: Boolean) {
            seeThroughWalls = seeThrough
        }

        fun setActive(active: Boolean) {
            if (isActive != active) {
                isActive = active
                if (!active) {
                    cleanup()
                }
            }
        }

        private fun cleanup() {
            if (thermalChain != null) {
                thermalChain!!.close()
                thermalChain = null
            }
        }

        fun isActive(): Boolean {
            return isActive
        }

        @SubscribeEvent
        fun onRenderLevel(event: RenderLevelStageEvent) {
            RenderSystem.setShaderGameTime(0, event.partialTick.getGameTimeDeltaPartialTick(true))

            if (!isActive) return

            if (event.stage === RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
                prepareAndRenderEntities(event.poseStack, event.partialTick.getGameTimeDeltaPartialTick(true))
            } else if (event.stage === RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                applyPostProcess(event.partialTick.getGameTimeDeltaPartialTick(true))
            }
        }

        private fun ensureChain(mc: Minecraft): Boolean {
            if (thermalChain == null) {
                try {
                    thermalChain = PostChain(
                        mc.textureManager,
                        mc.resourceManager,
                        mc.mainRenderTarget,
                        THERMAL_EFFECT
                    )
                    thermalChain!!.resize(mc.window.width, mc.window.height)
                    lastWidth = mc.window.width
                    lastHeight = mc.window.height
                } catch (e: Exception) {
                    e.printStackTrace()
                    isActive = false
                    return false
                }
            }

            if (lastWidth != mc.window.width || lastHeight != mc.window.height) {
                lastWidth = mc.window.width
                lastHeight = mc.window.height
                thermalChain!!.resize(lastWidth, lastHeight)
            }
            return true
        }

        private fun prepareAndRenderEntities(poseStack: PoseStack, partialTick: Float) {
            val mc = Minecraft.getInstance()
            if (mc.level == null) {
                return
            }

            if (!ensureChain(mc)) return

            val thermalBuffer: RenderTarget = thermalChain!!.getTempTarget("thermal_buffer") ?: return

            thermalBuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f)
            thermalBuffer.clear(Minecraft.ON_OSX)
            if (!seeThroughWalls) {
                if (mc.mainRenderTarget.isStencilEnabled && !thermalBuffer.isStencilEnabled) {
                    thermalBuffer.enableStencil()
                }

                try {
                    thermalBuffer.copyDepthFrom(mc.mainRenderTarget)
                } catch (_: Throwable) {
                    seeThroughWalls = true
                }
            }
            thermalBuffer.bindWrite(true)

            val camera = mc.gameRenderer.mainCamera
            val cameraPos = camera.position

            poseStack.pushPose()
            val bufferSource = mc.renderBuffers().bufferSource()

            RenderSystem.enablePolygonOffset()
            RenderSystem.polygonOffset(-1.0f, -1.0f)
            mc.entityRenderDispatcher.setRenderShadow(false)

            for (entity in mc.level!!.entitiesForRendering()) {
                if (isHotEntity(entity)) {
                    val lerpX = Mth.lerp(partialTick.toDouble(), entity.xo, entity.x)
                    val lerpY = Mth.lerp(partialTick.toDouble(), entity.yo, entity.y)
                    val lerpZ = Mth.lerp(partialTick.toDouble(), entity.zo, entity.z)

                    mc.entityRenderDispatcher.render(
                        entity,
                        lerpX - cameraPos.x,
                        lerpY - cameraPos.y,
                        lerpZ - cameraPos.z,
                        entity.getViewYRot(partialTick),
                        partialTick,
                        poseStack,
                        bufferSource,
                        15728880
                    )
                }
            }

            bufferSource.endBatch()
            RenderSystem.disablePolygonOffset()
            poseStack.popPose()

            mc.mainRenderTarget.bindWrite(true)
        }

        private fun applyPostProcess(partialTick: Float) {
            if (thermalChain == null) return

            try {
                thermalChain!!.process(partialTick)
            } catch (e: Exception) {
                e.printStackTrace()
                cleanup()
            }

            Minecraft.getInstance().mainRenderTarget.bindWrite(true)
        }

        private fun isHotEntity(entity: Entity?): Boolean {
            return false
            //        return (entity != Minecraft.getInstance().player || !Minecraft.getInstance().options.getCameraType().isFirstPerson());
        }
    }
}
