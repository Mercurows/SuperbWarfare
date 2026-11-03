package com.atsuishio.superbwarfare.tools

import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.floor

/**
 * From TAC-Z
 */
object HitboxHelper {
    private val PLAYER_EYE_HEIGHTS = WeakHashMap<Player, LinkedList<Float>>()

    // 玩家位置缓存表
    private val PLAYER_POSITION = WeakHashMap<Player, LinkedList<Vec3>>()

    // 玩家命中箱缓存表
    private val PLAYER_HITBOXES = WeakHashMap<Player, LinkedList<AABB>>()

    // 玩家速度缓存表
    private val PLAYER_VELOCITY = WeakHashMap<Player, LinkedList<Vec3>>()

    // 命中箱缓存 Tick 上限
    private val SAVE_TICK = floor(20 + 0.5)

    @JvmStatic
    fun onPlayerTick(player: Player) {
        if (player.isSpectator) {
            PLAYER_POSITION.remove(player)
            PLAYER_HITBOXES.remove(player)
            PLAYER_VELOCITY.remove(player)
            PLAYER_EYE_HEIGHTS.remove(player)
            return
        }
        val positions = PLAYER_POSITION.computeIfAbsent(player) { LinkedList<Vec3>() }
        val boxes = PLAYER_HITBOXES.computeIfAbsent(player) { LinkedList<AABB>() }
        val velocities = PLAYER_VELOCITY.computeIfAbsent(player) { LinkedList<Vec3>() }
        val eyes = PLAYER_EYE_HEIGHTS.computeIfAbsent(player) { LinkedList<Float>() }
        positions.addFirst(player.position())
        boxes.addFirst(player.boundingBox)
        velocities.addFirst(getPlayerVelocity(player))
        eyes.addFirst(player.eyeHeight)
        // Position 用于速度计算，所以只需要缓存 2 个位置
        if (positions.size > 2) {
            positions.removeLast()
        }
        // 命中箱和速度缓存数量限制
        if (boxes.size > SAVE_TICK) {
            boxes.removeLast()
            velocities.removeLast()
            eyes.removeLast()
        }
    }

    @JvmStatic
    fun onPlayerLoggedOut(player: Player) {
        PLAYER_POSITION.remove(player)
        PLAYER_HITBOXES.remove(player)
        PLAYER_VELOCITY.remove(player)
        PLAYER_EYE_HEIGHTS.remove(player)
    }

    @JvmStatic
    fun rewindTicks(shooter: Entity?): Int =
        if (shooter is ServerPlayer) floor(shooter.latency / 50.0 + 0.5).toInt().coerceIn(0, 19) else 0

    @JvmStatic
    fun getEyeHeight(player: Player, ticks: Int): Double {
        val eyes = PLAYER_EYE_HEIGHTS[player]
        return if (eyes.isNullOrEmpty()) player.eyeHeight.toDouble() else eyes[ticks.coerceIn(0, eyes.lastIndex)].toDouble()
    }

    @JvmStatic
    fun getCompensatedEyeY(player: ServerPlayer, shooter: Entity?): Double {
        val ticks = rewindTicks(shooter)
        val bounds = if (shooter is ServerPlayer) getBoundingBox(player, ticks) else player.boundingBox
        val velocity = if (shooter is ServerPlayer) getVelocity(player, ticks)
        else Vec3(player.x - player.xOld, player.y - player.yOld, player.z - player.zOld)
        // Sweeping the body box downward must not also move the historical head region downward.
        val movementOffset = if (player.vehicle == null) -2.0 else -3.0
        return bounds.minY + velocity.y * movementOffset + getEyeHeight(player, ticks)
    }

    /** Shared by bullets and beams: preserve the existing rewind, sweep and movement offsets. */
    @JvmStatic
    fun getCompensatedBoundingBox(entity: Entity, shooter: Entity?): AABB {
        var bounds = entity.boundingBox
        var velocity = Vec3(entity.x - entity.xOld, entity.y - entity.yOld, entity.z - entity.zOld)
        if (entity is ServerPlayer && shooter is ServerPlayer) {
            val ticks = rewindTicks(shooter)
            bounds = getBoundingBox(entity, ticks)
            velocity = getVelocity(entity, ticks)
        }
        if (entity is Player && !entity.isCrouching) bounds = bounds.expandTowards(0.0, 0.0625, 0.0)
        bounds = bounds.expandTowards(velocity)
        if (entity is ServerPlayer) {
            if (entity.vehicle != null) bounds = bounds.move(velocity.scale(1.5))
            bounds = bounds.move(velocity.scale(3.0))
        }
        if (entity.vehicle != null) bounds = bounds.move(velocity.scale(-2.5))
        return bounds.move(velocity.scale(-5.0))
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
        val boxes = PLAYER_HITBOXES[entity]
        if (!boxes.isNullOrEmpty()) {
            val index = ping.coerceIn(0, boxes.size - 1)
            return boxes[index]
        }
        return entity.boundingBox
    }

    @JvmStatic
    fun getVelocity(entity: Player, ping: Int): Vec3 {
        val velocities = PLAYER_VELOCITY[entity]
        if (!velocities.isNullOrEmpty()) {
            val index = ping.coerceIn(0, velocities.size - 1)
            return velocities[index]
        }
        return getPlayerVelocity(entity)
    }
}
