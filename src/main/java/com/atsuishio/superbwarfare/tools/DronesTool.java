package com.atsuishio.superbwarfare.tools;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Code based on @Mafuyu404's <a href="https://github.com/Mafuyu404/DiligentStalker">DiligentStalker</a>
 */
public class DronesTool {

    private final UUID playerUUID;
    private final int droneId;
    public final Level level;
    public static final ConcurrentHashMap<UUID, Integer> INSTANCE_MAP = new ConcurrentHashMap<>();

    private static final ConcurrentHashMap<UUID, DronesTool> INSTANCE_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, DronesTool> DRONES_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, UUID> DRONE_TO_PLAYER = new ConcurrentHashMap<>();

    // 缓存失效时间
    private static final long CACHE_EXPIRE_TIME = 5000;
    private long lastAccessTime;

    public DronesTool(UUID playerUUID, int droneId, Level level) {
        this.playerUUID = playerUUID;
        this.droneId = droneId;
        this.level = level;
        this.lastAccessTime = System.currentTimeMillis();
    }

    public Player getPlayer() {
        this.lastAccessTime = System.currentTimeMillis();
        return this.level.getPlayerByUUID(this.playerUUID);
    }

    public Entity getDrone() {
        this.lastAccessTime = System.currentTimeMillis();
        return this.level.getEntity(this.droneId);
    }

    public static DronesTool connect(Player player, Entity stalker) {
        if (player == null || stalker == null) return null;
        // 这边可能需要修改一下逻辑，不知道原代码部分是否允许1玩家对n无人机
        if (hasInstanceOf(player) || hasInstanceOf(stalker)) return null;

        if (player.level().isClientSide) {
            // TODO 向客户端发送连接的网络包
        }

        player.displayClientMessage(Component.translatable("tips.superbwarfare.monitor.linked").withStyle(ChatFormatting.GREEN), true);

        INSTANCE_MAP.put(player.getUUID(), stalker.getId());
        DRONE_TO_PLAYER.put(stalker.getId(), player.getUUID());

        DronesTool instance = new DronesTool(player.getUUID(), stalker.getId(), player.level());
        INSTANCE_CACHE.put(player.getUUID(), instance);
        DRONES_CACHE.put(stalker.getId(), instance);

        return instance;
    }

    public static DronesTool getInstanceOf(Entity entity) {
        if (entity == null) return null;

        UUID uuid = entity.getUUID();
        int id = entity.getId();

        DronesTool cache = INSTANCE_CACHE.get(uuid);
        if (cache != null && !isCacheExpired(cache)) {
            return cache;
        }

        cache = DRONES_CACHE.get(id);
        if (cache != null && !isCacheExpired(cache)) {
            return cache;
        }

        boolean isPlayer = INSTANCE_MAP.containsKey(uuid);
        boolean isDrone = DRONE_TO_PLAYER.containsKey(id);

        DronesTool instance = null;
        if (isPlayer) {
            var stalkerId = INSTANCE_MAP.get(uuid);
            if (stalkerId != null) {
                instance = new DronesTool(uuid, stalkerId, entity.level());
                INSTANCE_CACHE.put(uuid, instance);
            }
        } else if (isDrone) {
            UUID playerUUID = DRONE_TO_PLAYER.get(id);
            if (playerUUID != null) {
                instance = new DronesTool(playerUUID, id, entity.level());
                DRONES_CACHE.put(id, instance);
            }
        }

        return instance;
    }

    public void disconnect() {
        if (level.isClientSide) {
            // TODO 向客户端发送断开连接的网络包
        }
        INSTANCE_MAP.remove(this.playerUUID);
        DRONE_TO_PLAYER.remove(this.droneId);
        INSTANCE_CACHE.remove(this.playerUUID);
        DRONES_CACHE.remove(this.droneId);
    }

    public static boolean hasInstanceOf(Entity entity) {
        if (entity == null) return false;

        UUID uuid = entity.getUUID();
        if (INSTANCE_CACHE.containsKey(uuid) && !isCacheExpired(INSTANCE_CACHE.get(uuid))) {
            return true;
        }

        int id = entity.getId();
        if (DRONES_CACHE.containsKey(id) && !isCacheExpired(DRONES_CACHE.get(id))) {
            return true;
        }

        return (INSTANCE_MAP.containsKey(uuid) || INSTANCE_MAP.containsValue(id));
    }

    private static boolean isCacheExpired(DronesTool instance) {
        return System.currentTimeMillis() - instance.lastAccessTime > CACHE_EXPIRE_TIME;
    }

    public static void cleanupExpiredCache() {
        long currentTime = System.currentTimeMillis();
        INSTANCE_CACHE.entrySet().removeIf(entry ->
                currentTime - entry.getValue().lastAccessTime > CACHE_EXPIRE_TIME);
        DRONES_CACHE.entrySet().removeIf(entry ->
                currentTime - entry.getValue().lastAccessTime > CACHE_EXPIRE_TIME);
    }
}
