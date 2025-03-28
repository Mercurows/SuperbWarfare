package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.LevelEntityGetter;

import java.util.UUID;

public class EntityFindUtil {

    /**
     * 获取世界里的所有实体，对ClientLevel和ServerLevel均有效
     *
     * @param level 目标世界
     * @return 所有实体
     */
    public static LevelEntityGetter<Entity> getEntities(Level level) {
        if (level instanceof ServerLevel serverLevel) {
            return serverLevel.getEntities();
        }
        var clientLevel = (ClientLevel) level;
        return clientLevel.getEntities();
    }

    /**
     * 查找当前已知实体，对ClientLevel和ServerLevel均有效
     *
     * @param level      实体所在世界
     * @param uuidString 目标实体UUID字符串
     * @return 目标实体或null
     */
    public static Entity findEntity(Level level, String uuidString) {
        try {
            var uuid = UUID.fromString(uuidString);
            Entity target;

            if (level instanceof ServerLevel serverLevel) {
                target = serverLevel.getEntity(uuid);
            } else {
                var clientLevel = (ClientLevel) level;
                target = clientLevel.getEntities().get(uuid);
            }
            return target;
        } catch (Exception ignored) {
        }

        return null;
    }

    public static Player findPlayer(Level level, String uuidString) {
        var target = findEntity(level, uuidString);
        if (target instanceof Player player) {
            return player;
        }

        return null;
    }

    public static DroneEntity findDrone(Level level, String uuidString) {
        var target = findEntity(level, uuidString);
        if (target instanceof DroneEntity drone) {
            return drone;
        }

        return null;
    }

}
