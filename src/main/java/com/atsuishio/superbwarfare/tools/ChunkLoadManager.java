package com.atsuishio.superbwarfare.tools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChunkLoadManager {
    private static final Map<ServerLevel, Map<ChunkPos, Integer>> chunkRefCounts = new HashMap<>();
    private static final Map<ServerLevel, Set<ChunkPos>> forcedChunks = new HashMap<>();

    public static void forceChunk(ServerLevel level, ChunkPos pos) {
        // 初始化维度记录
        chunkRefCounts.computeIfAbsent(level, k -> new HashMap<>());
        forcedChunks.computeIfAbsent(level, k -> new HashSet<>());

        // 更新引用计数
        int count = chunkRefCounts.get(level).getOrDefault(pos, 0);
        chunkRefCounts.get(level).put(pos, count + 1);

        // 首次引用时强制加载区块
        if (count == 0) {
            level.setChunkForced(pos.x, pos.z, true);
            forcedChunks.get(level).add(pos);
        }
    }

    public static void releaseChunk(ServerLevel level, ChunkPos pos) {
        if (!chunkRefCounts.containsKey(level)) return;

        Map<ChunkPos, Integer> dimCounts = chunkRefCounts.get(level);
        if (!dimCounts.containsKey(pos)) return;

        // 更新引用计数
        int count = dimCounts.get(pos) - 1;
        if (count <= 0) {
            // 最后引用时卸载区块
            level.setChunkForced(pos.x, pos.z, false);
            dimCounts.remove(pos);
            forcedChunks.get(level).remove(pos);
        } else {
            dimCounts.put(pos, count);
        }
    }

    public static void debugInfo(ServerLevel level) {
        System.out.println("Loaded Chunks: ");
        if (chunkRefCounts.containsKey(level)) {
            chunkRefCounts.get(level).forEach((pos, count) ->
                    System.out.printf("  [%d, %d]: %d references%n", pos.x, pos.z, count));
        }
    }
}
