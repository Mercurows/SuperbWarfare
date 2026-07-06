package com.atsuishio.superbwarfare.client.renderer

import com.atsuishio.superbwarfare.client.DistantVehicleManager
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.LightTexture
import net.minecraft.util.Mth
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RenderLevelStageEvent

@EventBusSubscriber(Dist.CLIENT)
object DistantVehicleRenderer {

    private val FULL_SKY_LIGHT = LightTexture.pack(0, 15)

    @SubscribeEvent
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage !== RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
        val level = mc.level ?: return
        val ghosts = DistantVehicleManager.ghosts()
        if (ghosts.isEmpty()) return

        val partialTick = event.partialTick.getGameTimeDeltaPartialTick(false)
        val camPos = event.camera.position
        val poseStack = event.poseStack
        val bufferSource = mc.renderBuffers().bufferSource()
        val dispatcher = mc.entityRenderDispatcher
        val frustum = event.frustum

        // Призраки за пределами ванильного тумана — растягиваем его на время рендера
        val fogStart = RenderSystem.getShaderFogStart()
        val fogEnd = RenderSystem.getShaderFogEnd()
        RenderSystem.setShaderFogStart(Float.MAX_VALUE)
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE)

        var rendered = false
        for (ghost in ghosts) {
            // Техника вошла в ванильный tracking range — её рендерит ваниль
            if (level.getEntity(ghost.serverId) != null) continue

            val entity = ghost.entity
            if (frustum != null && !frustum.isVisible(entity.boundingBox.inflate(3.0))) continue

            val x = Mth.lerp(partialTick.toDouble(), entity.xo, entity.x)
            val y = Mth.lerp(partialTick.toDouble(), entity.yo, entity.y)
            val z = Mth.lerp(partialTick.toDouble(), entity.zo, entity.z)
            val yaw = Mth.lerp(partialTick, entity.yRotO, entity.yRot)

            dispatcher.render(
                entity,
                x - camPos.x, y - camPos.y, z - camPos.z,
                yaw, partialTick, poseStack, bufferSource, FULL_SKY_LIGHT,
            )
            rendered = true
        }

        if (rendered) {
            // Флашим до восстановления тумана: draw call происходит здесь
            bufferSource.endBatch()
        }
        RenderSystem.setShaderFogStart(fogStart)
        RenderSystem.setShaderFogEnd(fogEnd)
    }
}
