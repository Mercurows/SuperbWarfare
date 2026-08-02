package com.atsuishio.superbwarfare.client.renderer

import com.atsuishio.superbwarfare.client.ClientSyncedEntityHandler
import com.atsuishio.superbwarfare.config.server.SyncConfig
import com.atsuishio.superbwarfare.tools.clientLevel
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.shaders.FogShape
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.VertexSorting
import net.minecraft.client.renderer.LevelRenderer
import net.minecraft.core.BlockPos
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RenderLevelStageEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix4f

/**
 * 在 3D 世界中渲染超出玩家视距的同步实体。
 *
 * 在 [RenderLevelStageEvent.Stage.AFTER_ENTITIES] 阶段：
 * 1. 扩展投影矩阵远裁剪面（参考 RemoteEntityRenderHandler）
 * 2. 扩展雾曲线
 * 3. 渲染未在 clientLevel 中的同步实体
 * 4. 恢复雾和投影矩阵
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object SyncedEntityWorldRenderer {
    @SubscribeEvent
    fun onRenderLevelStage(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
        if (!SyncConfig.ENABLE_RENDER_SYNCED_ENTITIES.get()) return

        val level = clientLevel ?: return
        val camera = event.camera
        val dispatcher = mc.entityRenderDispatcher
        val bufferSource = mc.renderBuffers().bufferSource()
        val partialTick = event.partialTick

        val uniqueEntities = ClientSyncedEntityHandler.getSyncedWorldRenderEntities(level)

        bufferSource.endBatch()

        val savedProj = Matrix4f(RenderSystem.getProjectionMatrix())
        val savedFogStart = RenderSystem.getShaderFogStart()
        val savedFogEnd = RenderSystem.getShaderFogEnd()
        val savedFogShape = RenderSystem.getShaderFogShape()

        val extendedProj = createExtendedProjection(savedProj)
        RenderSystem.setProjectionMatrix(extendedProj, VertexSorting.DISTANCE_TO_ORIGIN)

        RenderSystem.setShaderFogStart(savedFogEnd)
        RenderSystem.setShaderFogEnd(SyncConfig.MAX_RENDER_DISTANCE.get().toFloat())
        RenderSystem.setShaderFogShape(FogShape.SPHERE)

        try {
            for (entity in uniqueEntities) {
                if (level.getEntity(entity.id) != null) continue

                val ix: Double
                val iy: Double
                val iz: Double

                val entry = ClientSyncedEntityHandler.getWorldRenderEntry(level, entity.id) ?: continue
                if (entry.entity.y < SyncConfig.MIN_RENDER_HEIGHT.get()) continue

                entity.xRotO = entity.xRot

                val elapsedTicks = ((System.currentTimeMillis() - entry.timeStamp) / 50.0)
                    .coerceIn(0.0, 2.0)
                ix = entity.x + entry.velocity.x * elapsedTicks
                iy = entity.y + entry.velocity.y * elapsedTicks
                iz = entity.z + entry.velocity.z * elapsedTicks

                val dx = ix - camera.position.x
                val dy = iy - camera.position.y
                val dz = iz - camera.position.z
                val distSq = dx * dx + dy * dy + dz * dz
                if (distSq > SyncConfig.MAX_RENDER_DISTANCE.get() * SyncConfig.MAX_RENDER_DISTANCE.get()) continue

                val blockPos = BlockPos.containing(ix, iy, iz)
                val packedLight = LevelRenderer.getLightColor(level, blockPos)

                val relX = ix - camera.position.x
                val relY = iy - camera.position.y
                val relZ = iz - camera.position.z

                dispatcher.render(
                    entity,
                    relX,
                    relY,
                    relZ,
                    entity.yRot,
                    partialTick,
                    event.poseStack,
                    bufferSource,
                    packedLight
                )
            }
        } finally {
            bufferSource.endBatch()
            RenderSystem.setProjectionMatrix(savedProj, VertexSorting.DISTANCE_TO_ORIGIN)
            RenderSystem.setShaderFogStart(savedFogStart)
            RenderSystem.setShaderFogEnd(savedFogEnd)
            RenderSystem.setShaderFogShape(savedFogShape)
        }
    }

    /**
     * 从当前投影矩阵创建扩展远裁剪面的新矩阵。
     * 保留原有 FOV、aspect、near，仅将 far 替换为 [SyncConfig.MAX_RENDER_DISTANCE] + 512。
     */
    private fun createExtendedProjection(projection: Matrix4f): Matrix4f {
        val extended = Matrix4f(projection)
        val m22 = projection.m22()
        val m32 = projection.m32()
        val near = m32 / (m22 - 1f)
        val far = SyncConfig.MAX_RENDER_DISTANCE.get().toFloat() + 512f
        extended.m22(-(far + near) / (far - near))
        extended.m32(-(2f * far * near) / (far - near))
        return extended
    }
}
