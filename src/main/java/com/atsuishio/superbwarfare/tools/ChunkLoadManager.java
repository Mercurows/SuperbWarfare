package com.atsuishio.superbwarfare.tools;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

public class ChunkLoadManager {
    private static final Map<ServerLevel, Map<ChunkPos, Set<UUID>>> chunkEntities = new HashMap<>();

    public static void forceChunk(ServerLevel level, ChunkPos pos, UUID entityId) {
        chunkEntities.computeIfAbsent(level, k -> new HashMap<>());

        Map<ChunkPos, Set<UUID>> dimMap = chunkEntities.get(level);
        Set<UUID> entities = dimMap.computeIfAbsent(pos, k -> new HashSet<>());

        entities.add(entityId);

        // 首次加载时强制区块
        if (entities.size() == 1) {
            level.setChunkForced(pos.x, pos.z, true);
        }
    }

    public static void releaseChunk(ServerLevel level, ChunkPos pos, UUID entityId) {
        if (!chunkEntities.containsKey(level)) return;

        Map<ChunkPos, Set<UUID>> dimMap = chunkEntities.get(level);
        if (!dimMap.containsKey(pos)) return;

        Set<UUID> entities = dimMap.get(pos);
        entities.remove(entityId);

        if (entities.isEmpty()) {
            level.setChunkForced(pos.x, pos.z, false);
            dimMap.remove(pos);
        }
    }

    public static void releaseAllForEntity(ServerLevel level, UUID entityId) {
        if (!chunkEntities.containsKey(level)) return;

        Map<ChunkPos, Set<UUID>> dimMap = chunkEntities.get(level);
        List<ChunkPos> toRemove = new ArrayList<>();

        for (Map.Entry<ChunkPos, Set<UUID>> entry : dimMap.entrySet()) {
            Set<UUID> entities = entry.getValue();
            if (entities.remove(entityId) && entities.isEmpty()) {
                toRemove.add(entry.getKey());
                level.setChunkForced(entry.getKey().x, entry.getKey().z, false);
            }
        }

        for (ChunkPos pos : toRemove) {
            dimMap.remove(pos);
        }
    }
}
