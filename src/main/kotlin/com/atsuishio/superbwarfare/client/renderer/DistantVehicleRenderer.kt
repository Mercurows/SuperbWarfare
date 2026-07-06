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
        val frustum = event.frustum

        // Отбираем только призраки, которые реально нужно рисовать (дедупликация + frustum)
        val visible = DistantVehicleManager.ghosts().filter { ghost ->
            level.getEntity(ghost.serverId) == null &&
                (frustum == null || frustum.isVisible(ghost.entity.boundingBox.inflate(3.0)))
        }
        // Нечего рисовать — не трогаем туман и не флашим ванильные батчи
        if (visible.isEmpty()) return

        val partialTick = event.partialTick.getGameTimeDeltaPartialTick(false)
        val camPos = event.camera.position
        val poseStack = event.poseStack
        val bufferSource = mc.renderBuffers().bufferSource()
        val dispatcher = mc.entityRenderDispatcher

        // Сначала флашим ванильные батчи с правильным туманом,
        // чтобы ближние объекты не рендерились без тумана
        bufferSource.endBatch()

        // Теперь расширяем туман только на время рендера призраков
        val fogStart = RenderSystem.getShaderFogStart()
        val fogEnd = RenderSystem.getShaderFogEnd()
        RenderSystem.setShaderFogStart(Float.MAX_VALUE)
        RenderSystem.setShaderFogEnd(Float.MAX_VALUE)

        for (ghost in visible) {
            val entity = ghost.entity
            val x = Mth.lerp(partialTick.toDouble(), entity.xo, entity.x)
            val y = Mth.lerp(partialTick.toDouble(), entity.yo, entity.y)
            val z = Mth.lerp(partialTick.toDouble(), entity.zo, entity.z)
            val yaw = Mth.lerp(partialTick, entity.yRotO, entity.yRot)

            dispatcher.render(
                entity,
                x - camPos.x, y - camPos.y, z - camPos.z,
                yaw, partialTick, poseStack, bufferSource, FULL_SKY_LIGHT,
            )
        }

        // Флашим призраки до восстановления тумана: draw call происходит здесь
        bufferSource.endBatch()
        RenderSystem.setShaderFogStart(fogStart)
        RenderSystem.setShaderFogEnd(fogEnd)
    }
}
