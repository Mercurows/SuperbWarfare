package com.atsuishio.superbwarfare.tools

import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.sqrt

object ProjectileCalculator {
    // 水平采样步长（方块），沿弹道地面投影每隔此距离检测一次地形高度
    private const val HORIZONTAL_STEP = 1.0
    // 最大采样步数，防止极端情况下死循环
    private const val MAX_STEPS = 4096

    /**
     * 计算炮弹精确落点位置（Vec3）
     *
     * 使用高度图辅助的抛物线采样算法替代逐步射线模拟。
     * 弹丸在恒定重力下沿抛物线运动，沿水平面投影以 [HORIZONTAL_STEP] 方块步长采样，
     * 通过高度图（O(1)）与弹道高度比对，找到落点区域后仅做一次精确射线检测。
     * 将每调用数千次 level.clip() 降低到 1~2 次。
     *
     * @param level     世界对象
     * @param startPos  发射点位置
     * @param launchVector 发射方向向量
     * @param velocity  初速度标量
     * @param gravity   重力加速度（负值表示向下）
     * @return 精确的落点位置，如果没有碰撞则返回最后位置
     */
    @JvmStatic
    fun calculatePreciseImpactPoint(
        level: Level,
        startPos: Vec3,
        launchVector: Vec3,
        velocity: Double,
        gravity: Double
    ): Vec3 {
        val dir = launchVector.normalize()
        val vx = dir.x * velocity
        val vy = dir.y * velocity
        val vz = dir.z * velocity

        val horizontalSpeed = sqrt(vx * vx + vz * vz)

        // 纯垂直运动（无水平位移）：直接检测脚下地形
        if (horizontalSpeed < 1e-6) {
            return calculateVerticalImpact(level, startPos)
        }

        // 水平步进的时间增量：每前进 HORIZONTAL_STEP 方块水平距离所需的时间
        val dt = HORIZONTAL_STEP / horizontalSpeed

        var t = 0.0
        var prevX = startPos.x
        var prevY = startPos.y
        var prevZ = startPos.z

        for (step in 0 until MAX_STEPS) {
            t += dt
            val x = startPos.x + vx * t
            // y(t) = y0 + vy*t + 0.5*g*t²  注意 gravity 为负值
            val y = startPos.y + vy * t + 0.5 * gravity * t * t
            val z = startPos.z + vz * t

            // 掉出世界底部：对最后一段做精确射线检测
            if (y < level.minBuildHeight) {
                return raycastOrClamp(
                    level,
                    Vec3(prevX, prevY, prevZ),
                    Vec3(x, y, z),
                    level.minBuildHeight.toDouble()
                )
            }

            // 通过高度图检测是否已抵达地形表面
            val blockX = x.toInt()
            val blockZ = z.toInt()

            if (level.hasChunk(blockX shr 4, blockZ shr 4)) {
                val terrainHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ).toDouble()
                if (y <= terrainHeight) {
                    // 找到落点区域，做精确射线检测
                    return preciseImpactRaycast(
                        level,
                        Vec3(prevX, prevY, prevZ),
                        Vec3(x, y, z),
                        terrainHeight
                    )
                }
            }

            prevX = x
            prevY = y
            prevZ = z
        }

        // 超过最大步数，返回最后计算位置
        return Vec3(prevX, prevY, prevZ)
    }

    // ---- 纯垂直运动 ----

    /**
     * 处理无水平速度分量的纯垂直运动。
     * 从弹丸位置上方垂直向下做一次射线检测即可获得精确落点。
     */
    private fun calculateVerticalImpact(
        level: Level,
        startPos: Vec3
    ): Vec3 {
        val bx = startPos.x.toInt()
        val bz = startPos.z.toInt()

        if (!level.hasChunk(bx shr 4, bz shr 4)) {
            return fallbackToWorldBottom(startPos, level.minBuildHeight)
        }

        val terrainHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz).toDouble()

        // 从地形上方垂直向下射线，精准捕获地形表面
        val aboveY = max(startPos.y, terrainHeight) + 2
        val belowY = level.minBuildHeight.toDouble() - 1
        val hit = level.clip(
            ClipContext(
                Vec3(startPos.x, aboveY, startPos.z),
                Vec3(startPos.x, belowY, startPos.z),
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.ANY,
                null
            )
        )
        if (hit.type == HitResult.Type.BLOCK) return hit.location

        return Vec3(startPos.x, terrainHeight, startPos.z)
    }

    // ---- 辅助方法 ----

    /**
     * 对弹丸离开世界底部前最后一段轨迹做精确射线检测。
     */
    private fun raycastOrClamp(
        level: Level,
        start: Vec3,
        end: Vec3,
        minY: Double
    ): Vec3 {
        // 确保射线终点不低于世界底部
        val clampedEnd = Vec3(end.x, max(end.y, minY - 1), end.z)
        val hit = level.clip(
            ClipContext(start, clampedEnd, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, null)
        )
        if (hit.type == HitResult.Type.BLOCK) return hit.location
        return Vec3(end.x, minY, end.z)
    }

    /**
     * 落地点的精确射线检测。
     *
     * 采用两种策略：
     * 1. 从地形上方垂直向下检测（可处理大多数地形）
     * 2. 沿弹道方向检测（处理垂直面如悬崖、墙壁）
     */
    private fun preciseImpactRaycast(
        level: Level,
        prev: Vec3,
        current: Vec3,
        terrainHeight: Double
    ): Vec3 {
        // 策略1：从地形表面上方垂直向下射线
        val above = Vec3(current.x, terrainHeight + 2, current.z)
        val below = Vec3(current.x, terrainHeight - 2, current.z)
        val hit = level.clip(
            ClipContext(above, below, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, null)
        )
        if (hit.type == HitResult.Type.BLOCK) return hit.location

        // 策略2：沿弹道方向检测（处理地形垂直面）
        val hit2 = level.clip(
            ClipContext(prev, current, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, null)
        )
        if (hit2.type == HitResult.Type.BLOCK) return hit2.location

        // 无碰撞方块时回退到高度图位置
        return Vec3(current.x, terrainHeight, current.z)
    }

    /**
     * 区块未加载时的回退：直接使用世界底部高度。
     */
    private fun fallbackToWorldBottom(startPos: Vec3, minBuildHeight: Int): Vec3 {
        return Vec3(startPos.x, minBuildHeight.toDouble(), startPos.z)
    }
}
