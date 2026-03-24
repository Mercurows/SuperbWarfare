package com.atsuishio.superbwarfare.tools;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClientEntityTracker {
    // 存储实体信息：UUID -> EntityData
    private static final Map<Integer, EntityData> trackedEntities = new ConcurrentHashMap<>();

    public static void updateEntity(int id, ResourceLocation dimension, double x, double y, double z) {
        trackedEntities.put(id, new EntityData(id, dimension, x, y, z));
    }

    public static void removeEntity(UUID uuid) {
        trackedEntities.remove(uuid);
    }

    public static Collection<EntityData> getAllTracked() {
        return trackedEntities.values();
    }

    // 可选：根据维度过滤，只显示当前维度的实体
    public static List<EntityData> getInCurrentDimension(Level level) {
        ResourceLocation currentDim = level.dimension().location();
        return trackedEntities.values().stream()
                .filter(data -> data.dimension().equals(currentDim))
                .collect(Collectors.toList());
    }

    public record EntityData(int id, ResourceLocation dimension, double x, double y, double z) {}
}