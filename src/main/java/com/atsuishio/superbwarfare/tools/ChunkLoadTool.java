package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.Mod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.world.ForgeChunkManager;

import java.util.HashSet;
import java.util.Set;

public class ChunkLoadTool {

    /**
     * 根据动量计算需要加载的区块并卸载不再需要加载的区块
     */
    public static void updateLoadedChunks(ServerLevel level, Entity entity, Set<Long> loadedChunks) {
        var x = entity.position().x;
        var z = entity.position().z;

        var nextX = x + entity.getDeltaMovement().x;
        var nextZ = z + entity.getDeltaMovement().z;

        // 加载当前区块和下一tick会进入的区块
        var newChunks = new HashSet<Long>();
        newChunks.add(ChunkPos.asLong(new BlockPos((int) x, 0, (int) z)));
        newChunks.add(ChunkPos.asLong(new BlockPos((int) nextX, 0, (int) nextZ)));

        // 计算需要更新的区块
        var chunksToLoad = newChunks.stream().filter(chunk -> !loadedChunks.contains(chunk)).toList();
        var chunksToUnload = loadedChunks.stream().filter(chunk -> !newChunks.contains(chunk)).toList();

        chunksToLoad.forEach(chunk -> {
            var chunkPos = new ChunkPos(chunk);
            ForgeChunkManager.forceChunk(level, Mod.MODID, entity, chunkPos.x, chunkPos.z, true, false);
        });

        chunksToUnload.forEach(chunk -> {
            var chunkPos = new ChunkPos(chunk);
            Mod.queueServerWork(10, () -> {
                ForgeChunkManager.forceChunk(level, Mod.MODID, entity, chunkPos.x, chunkPos.z, false, false);
            });
        });

        loadedChunks.clear();
        loadedChunks.addAll(newChunks);
    }

    /**
     * 卸载所有已加载区块
     */
    public static void unloadAllChunks(ServerLevel level, Entity entity, Set<Long> loadedChunks) {
        loadedChunks.forEach(chunk -> {
            var chunkPos = new ChunkPos(chunk);
            Mod.queueServerWork(20, () -> {
                ForgeChunkManager.forceChunk(level, Mod.MODID, entity, chunkPos.x, chunkPos.z, false, false);
            });
        });
    }
}
