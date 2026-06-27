package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.config.server.MiscConfig
import com.atsuishio.superbwarfare.tools.ChunkLoadManager.updateEntityChunks
import net.minecraft.core.BlockPos
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.concurrent.ConcurrentHashMap

/**
 * 引用计数区块加载管理器。
 *
 * 弹丸每 tick 通过 [updateEntityChunks] 声明自己需要的区块；
 * 当多个实体需要同一区块时，引用计数 > 1，只有全部离开后才会卸载。
 *
 * 定期清理已消失实体的引用（与 ServerSyncedEntityHandler 共用清理间隔）。
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
object ChunkLoadManager {

    /** ChunkPos → 引用该区块的实体 ID 集合 */
    private val chunkRefCounts = ConcurrentHashMap<ChunkPos, MutableSet<Int>>()
    /** entityId → 该实体当前持有的区块集合 */
    private val entityChunks = ConcurrentHashMap<Int, MutableSet<ChunkPos>>()

    // ── Public API ──

    /**
     * 每 tick 由弹丸调用，声明当前需要的区块（当前位置 + 前方预加载）。
     * 自动计算新旧差集：离开的区块 release，新进入的区块 acquire。
     */
    @JvmStatic
    fun updateEntityChunks(entity: Entity, positions: Collection<Vec3>) {
        val level = entity.level() as? ServerLevel ?: return
        val newChunks = positions.mapTo(mutableSetOf()) { ChunkPos(BlockPos.containing(it)) }
        val oldChunks = entityChunks.getOrPut(entity.id) { mutableSetOf() }

        // 释放已离开的区块
        for (pos in oldChunks - newChunks) {
            releaseChunk(level, pos, entity.id)
        }
        // 获取新进入的区块
        for (pos in newChunks - oldChunks) {
            acquireChunk(level, pos, entity.id)
        }

        // 更新追踪
        oldChunks.clear()
        oldChunks.addAll(newChunks)
    }

    /**
     * 实体销毁时调用，释放其所有区块引用并清理双向索引。
     */
    @JvmStatic
    fun removeEntity(entity: Entity) {
        val level = entity.level() as? ServerLevel ?: return
        val chunks = entityChunks.remove(entity.id) ?: return
        for (pos in chunks) {
            releaseChunk(level, pos, entity.id)
        }
    }

    // ── Internal ──

    private fun acquireChunk(level: ServerLevel, pos: ChunkPos, entityId: Int) {
        val refs = chunkRefCounts.getOrPut(pos) { mutableSetOf() }
        val wasEmpty = refs.isEmpty()
        refs.add(entityId)
        if (wasEmpty) {
            level.chunkSource.updateChunkForced(pos, true)
        }
    }

    private fun releaseChunk(level: ServerLevel, pos: ChunkPos, entityId: Int) {
        val refs = chunkRefCounts[pos] ?: return
        refs.remove(entityId)
        if (refs.isEmpty()) {
            level.chunkSource.updateChunkForced(pos, false)
            chunkRefCounts.remove(pos)
        }
    }

    // ── Cleanup ──

    @JvmStatic
    fun cleanup(server: MinecraftServer) {
        for (dimLevel in server.allLevels) {
            val toRemove = mutableListOf<ChunkPos>()
            chunkRefCounts.forEach { (pos, refs) ->
                refs.removeIf { entityId -> dimLevel.getEntity(entityId) == null }
                if (refs.isEmpty()) {
                    toRemove.add(pos)
                }
            }
            for (pos in toRemove) {
                (dimLevel as ServerLevel).chunkSource.updateChunkForced(pos, false)
                chunkRefCounts.remove(pos)
            }
        }
        // 同步清理 entityChunks 中的死引用
        entityChunks.keys.removeIf { entityId ->
            server.allLevels.none { it.getEntity(entityId) != null }
        }
    }

    // ── Periodic tick ──

    @SubscribeEvent
    fun onServerTick(event: TickEvent.ServerTickEvent) {
        if (event.phase == TickEvent.Phase.START) return
        val server = event.server
        if (server.tickCount % MiscConfig.SERVER_SYNC_CLEAN_INTERVAL.get() == 0) {
            cleanup(server)
        }
    }
}
