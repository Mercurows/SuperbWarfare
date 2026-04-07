package com.atsuishio.superbwarfare.tools

import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import java.util.*

object ProjectileCalculator {
    private const val TIME_STEP = 1 // 时间步长（刻）
    private const val MAX_ITERATIONS = 512 // 最大迭代次数

    /**
     * 计算炮弹精确落点位置（Vec3）
     * 
     * @param level 世界对象
     * @param startPos 发射点位置（Vec3）
     * @param launchVector 发射向量（Vec3）
     * @return 精确的落点位置（Vec3），如果没有碰撞则返回最后位置
     */
    @JvmStatic
    fun calculatePreciseImpactPoint(
        level: Level,
        startPos: Vec3,
        launchVector: Vec3,
        velocity: Double,
        gravity: Double
    ): Vec3 {
        var currentPos = startPos
        var currentVelocity = launchVector.normalize().scale(velocity)
        var previousPos = startPos

        repeat(MAX_ITERATIONS) {
            // 计算下一个位置
            val nextPos = currentPos.add(
                currentVelocity.x * TIME_STEP,
                currentVelocity.y * TIME_STEP,
                currentVelocity.z * TIME_STEP
            )

            // 应用重力
            currentVelocity = currentVelocity.add(0.0, gravity * TIME_STEP, 0.0)

            // 检查碰撞
            val collisionPoint = checkCollision(level, previousPos, nextPos)

            if (collisionPoint.isPresent) {
                // 精确计算碰撞点
                return refineCollisionPoint(level, previousPos, collisionPoint.get())
            }

            // 边界检查
            if (nextPos.y < level.minBuildHeight) {
                return Vec3(nextPos.x, level.minBuildHeight.toDouble(), nextPos.z)
            }

            // 更新位置
            previousPos = currentPos
            currentPos = nextPos
        }

        // 超过最大迭代次数，返回最后位置
        return currentPos
    }

    /**
     * 检查两点之间是否有碰撞
     */
    private fun checkCollision(level: Level, start: Vec3, end: Vec3): Optional<Vec3> {
        // 使用Minecraft内置的光线追踪进行碰撞检测
        val hitResult = level.clip(
            ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,  // 只检测碰撞方块
                ClipContext.Fluid.ANY,  // 忽略流体
                null // 无实体
            )
        )

        // 如果检测到碰撞，返回碰撞点
        if (hitResult.type == HitResult.Type.BLOCK) {
            return Optional.of(hitResult.getLocation())
        }

        // 没有检测到碰撞
        return Optional.empty()
    }

    /**
     * 精确计算碰撞点（使用二分法提高精度）
     */
    private fun refineCollisionPoint(level: Level, safePoint: Vec3, collisionPoint: Vec3): Vec3 {
        var low = safePoint
        var high = collisionPoint
        var bestPoint = collisionPoint

        // 二分法迭代提高精度
        repeat(9) {
            val mid = low.add(high.subtract(low).scale(0.5))

            // 检查从安全点到中点是否有碰撞
            val collision = checkCollision(level, low, mid)

            if (collision.isPresent) {
                // 有碰撞，将高点移动到中点
                high = mid
                bestPoint = collision.get()
            } else {
                // 无碰撞，将低点移动到中点
                low = mid
            }
        }

        return bestPoint
    }
}
