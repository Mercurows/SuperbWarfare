package com.atsuishio.superbwarfare.client.shader

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.PostChain
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.server.packs.resources.ResourceManagerReloadListener
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
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
        private var vehicleMode = false

        // PJM: динамическая интенсивность помех (0..1.5). Считается на клиенте
        // (ClientEventHandler.handleThermalImaging) из выстрела и HP техники, читается в PostPassMixin.
        private var interference = 0.0f

        @JvmStatic
        fun setInterference(value: Float) {
            interference = value
        }

        @JvmStatic
        fun getInterference(): Float = interference

        fun setSeeThroughWalls(seeThrough: Boolean) {
            seeThroughWalls = seeThrough
        }

        /**
         * Vehicle sights use a lower-resolution thermal sensor.  Goggles retain the clean image.
         */
        @JvmStatic
        fun setVehicleMode(enabled: Boolean) {
            vehicleMode = enabled
        }

        @JvmStatic
        fun isVehicleMode(): Boolean = vehicleMode

        fun setActive(active: Boolean) {
            if (isActive != active) {
                isActive = active
                if (!active) {
                    vehicleMode = false
                    interference = 0.0f
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
            // PJM: вернуть тени, иначе setRenderShadow(false) выше глушит тени во всём мире,
            // пока активен тепловизор.
            mc.entityRenderDispatcher.setRenderShadow(true)
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
            if (entity == null) return false
            // PJM: «горячими» (яркими на тёмном фоне) считаем живых существ и технику SBW.
            // Своего игрока от первого лица не рисуем — иначе засветит весь экран моделью рук/тела.
            val mc = Minecraft.getInstance()
            if (entity === mc.player && mc.options.cameraType.isFirstPerson) return false
            return entity is LivingEntity || entity is VehicleEntity
        }
    }
}
