package com.atsuishio.superbwarfare.tools

import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.floor

/**
 * From TAC-Z
 */
object HitboxHelper {
    // 玩家位置缓存表
    private val PLAYER_POSITION = WeakHashMap<Player, LinkedList<Vec3>>()

    // 玩家命中箱缓存表
    private val PLAYER_HITBOXES = WeakHashMap<Player, LinkedList<AABB>>()

    // 玩家速度缓存表
    private val PLAYER_VELOCITY = WeakHashMap<Player, LinkedList<Vec3>>()

    // 命中箱缓存 Tick 上限
    private val SAVE_TICK = floor(20 + 0.5)

    fun onPlayerTick(player: Player) {
        if (player.isSpectator) {
            PLAYER_POSITION.remove(player)
            PLAYER_HITBOXES.remove(player)
            PLAYER_VELOCITY.remove(player)
            return
        }
        val positions = PLAYER_POSITION.computeIfAbsent(player) { LinkedList<Vec3>() }
        val boxes = PLAYER_HITBOXES.computeIfAbsent(player) { LinkedList<AABB>() }
        val velocities = PLAYER_VELOCITY.computeIfAbsent(player) { LinkedList<Vec3>() }
        positions.addFirst(player.position())
        boxes.addFirst(player.boundingBox)
        velocities.addFirst(getPlayerVelocity(player))
        // Position 用于速度计算，所以只需要缓存 2 个位置
        if (positions.size > 2) {
            positions.removeLast()
        }
        // 命中箱和速度缓存数量限制
        if (boxes.size > SAVE_TICK) {
            boxes.removeLast()
            velocities.removeLast()
        }
    }

    fun onPlayerLoggedOut(player: Player) {
        PLAYER_POSITION.remove(player)
        PLAYER_HITBOXES.remove(player)
        PLAYER_VELOCITY.remove(player)
    }

    @JvmStatic
    fun getPlayerVelocity(entity: Player): Vec3 {
        val positions = PLAYER_POSITION.computeIfAbsent(entity) { LinkedList<Vec3>() }
        if (positions.size > 1) {
            val currPos = positions.first()
            val prevPos = positions.last()
            return Vec3(currPos.x - prevPos.x, currPos.y - prevPos.y, currPos.z - prevPos.z)
        }
        return Vec3(0.0, 0.0, 0.0)
    }

    @JvmStatic
    fun getBoundingBox(entity: Player, ping: Int): AABB {
        if (PLAYER_HITBOXES.containsKey(entity)) {
            val boxes = PLAYER_HITBOXES[entity]!!
            val index = ping.coerceIn(0, boxes.size - 1)
            return boxes[index]
        }
        return entity.boundingBox
    }

    @JvmStatic
    fun getVelocity(entity: Player, ping: Int): Vec3 {
        if (PLAYER_VELOCITY.containsKey(entity)) {
            val velocities = PLAYER_VELOCITY[entity]!!
            val index = ping.coerceIn(0, velocities.size - 1)
            return velocities[index]
        }
        return getPlayerVelocity(entity)
    }
}