package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.entity.LevelEntityGetter
import java.util.*

object EntityFindUtil {
    /**
     * 获取世界里的所有实体，对ClientLevel和ServerLevel均有效
     * 
     * @param level 目标世界
     * @return 所有实体
     */
    @JvmStatic
    fun getEntities(level: Level): LevelEntityGetter<Entity> {
        if (level is ServerLevel) {
            return level.entities
        }
        val clientLevel = level as ClientLevel
        return clientLevel.entities
    }

    /**
     * 查找当前已知实体，对ClientLevel和ServerLevel均有效
     *
     * Pre-validates the UUID string before calling [UUID.fromString] to avoid
     * the extremely expensive [IllegalArgumentException] + [Throwable.fillInStackTrace]
     * path that was observed costing ~900ms cumulative in production profiling.
     * A valid UUID string is always exactly 36 characters (8-4-4-4-12 with hyphens);
     * common non-UUID sentinels like "undefined" (length 9) are rejected instantly.
     * 
     * @param level      实体所在世界
     * @param uuidString 目标实体UUID字符串
     * @return 目标实体或null
     */
    @JvmStatic
    fun findEntity(level: Level, uuidString: String?): Entity? {
        // Fast rejection: a valid UUID is always exactly 36 chars.
        // This filters "undefined", "", and other non-UUID sentinels
        // without entering the try/catch + stack-trace-fill path.
        if (uuidString == null || uuidString.length != 36) return null
        val uuid = try {
            UUID.fromString(uuidString)
        } catch (_: IllegalArgumentException) {
            return null
        }

        return if (level is ServerLevel) {
            level.getEntity(uuid)
        } else {
            (level as ClientLevel).entities.get(uuid)
        }
    }

    @JvmStatic
    fun findPlayer(level: Level, uuidString: String): Player? {
        val target = findEntity(level, uuidString)
        if (target is Player) {
            return target
        }
        return null
    }

    @JvmStatic
    fun findDrone(level: Level, uuidString: String): DroneEntity? {
        val target = findEntity(level, uuidString)
        if (target is DroneEntity) {
            return target
        }
        return null
    }
}