package com.atsuishio.superbwarfare.tools

import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil

fun AABB.showEdges(
    level: Level,
    particle: ParticleOptions = ParticleTypes.END_ROD,
    step: Double = 0.25,
    sendToAll: Boolean = false
) {
    TestTool.renderAABBEdgesWithParticles(level, this, particle, step, sendToAll)
}

object TestTool {
    @JvmStatic
    fun renderAABBEdgesWithParticles(
        level: Level,
        aabb: AABB,
        particle: ParticleOptions = ParticleTypes.END_ROD,
        step: Double = 0.25,
        sendToAll: Boolean = false
    ) {
        if (level !is ServerLevel) return
        if (level.gameTime % 2 == 0L) return

        val minX = aabb.minX
        val minY = aabb.minY
        val minZ = aabb.minZ
        val maxX = aabb.maxX
        val maxY = aabb.maxY
        val maxZ = aabb.maxZ

        val edges = listOf(
            Vec3(minX, minY, minZ) to Vec3(maxX, minY, minZ),
            Vec3(maxX, minY, minZ) to Vec3(maxX, minY, maxZ),
            Vec3(maxX, minY, maxZ) to Vec3(minX, minY, maxZ),
            Vec3(minX, minY, maxZ) to Vec3(minX, minY, minZ),

            Vec3(minX, maxY, minZ) to Vec3(maxX, maxY, minZ),
            Vec3(maxX, maxY, minZ) to Vec3(maxX, maxY, maxZ),
            Vec3(maxX, maxY, maxZ) to Vec3(minX, maxY, maxZ),
            Vec3(minX, maxY, maxZ) to Vec3(minX, maxY, minZ),

            Vec3(minX, minY, minZ) to Vec3(minX, maxY, minZ),
            Vec3(maxX, minY, minZ) to Vec3(maxX, maxY, minZ),
            Vec3(maxX, minY, maxZ) to Vec3(maxX, maxY, maxZ),
            Vec3(minX, minY, maxZ) to Vec3(minX, maxY, maxZ)
        )

        val zeroVelocity = Vec3.ZERO

        val players = if (sendToAll) {
            level.players()
        } else {
            val center = aabb.center
            level.players().filter { it.distanceToSqr(center.x, center.y, center.z) <= 64.0 * 64.0 }
        }

        for ((start, end) in edges) {
            val direction = end.subtract(start)
            val length = direction.length()
            val steps = ceil(length / step).toInt().coerceAtLeast(1)

            for (i in 0..steps) {
                val t = i.toDouble() / steps
                val point = start.add(direction.scale(t))

                for (player in players) {
                    level.sendParticles(
                        player,
                        particle,
                        true,
                        point.x, point.y, point.z,
                        1,
                        zeroVelocity.x, zeroVelocity.y, zeroVelocity.z,
                        0.0
                    )
                }
            }
        }
    }
}