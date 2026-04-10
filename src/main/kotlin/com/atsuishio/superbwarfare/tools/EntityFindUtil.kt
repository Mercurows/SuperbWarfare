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
     * @param level      实体所在世界
     * @param uuidString 目标实体UUID字符串
     * @return 目标实体或null
     */
    @JvmStatic
    fun findEntity(level: Level, uuidString: String?): Entity? {
        if (uuidString == null) return null
        try {
            val uuid = UUID.fromString(uuidString)
            val target: Entity?

            if (level is ServerLevel) {
                target = level.getEntity(uuid)
            } else {
                val clientLevel = level as ClientLevel
                target = clientLevel.entities.get(uuid)
            }
            return target
        } catch (_: Exception) {
        }
        return null
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
