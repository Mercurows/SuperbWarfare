package com.atsuishio.superbwarfare.client.renderer

import com.atsuishio.superbwarfare.client.DistantVehicleManager
import com.atsuishio.superbwarfare.client.compat.VoxyIntegration
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.renderer.LightTexture
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
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

        // Слайдер "SBW entity distance" в настройках Voxy; 0 = выключено
        val maxDistance = VoxyIntegration.distantEntityRenderDistance()
        if (maxDistance <= 0) return
        val maxDistanceSq = maxDistance.toDouble() * maxDistance

        val frustum = event.frustum
        val camPos = event.camera.position

        // Отбираем только призраки, которые реально нужно рисовать
        // (дедупликация с ванильным трекингом + лимит дистанции + frustum)
        fun shouldRender(serverId: Int, entity: Entity, inflate: Double): Boolean {
            if (level.getEntity(serverId) != null) return false
            if (entity.position().distanceToSqr(camPos) > maxDistanceSq) return false
            return frustum == null || frustum.isVisible(entity.boundingBox.inflate(inflate))
        }

        val visible = ArrayList<Entity>()
        for (ghost in DistantVehicleManager.ghosts()) {
            if (shouldRender(ghost.serverId, ghost.entity, 3.0)) visible += ghost.entity
        }
        for (ghost in DistantVehicleManager.projectileGhosts()) {
            // Снаряды мелкие и быстрые — раздуваем AABB сильнее, чтобы frustum не мерцал
            if (shouldRender(ghost.serverId, ghost.entity, 5.0)) visible += ghost.entity
        }
        // Нечего рисовать — не трогаем туман и не флашим ванильные батчи
        if (visible.isEmpty()) return

        val partialTick = event.partialTick.getGameTimeDeltaPartialTick(false)
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

        for (entity in visible) {
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
