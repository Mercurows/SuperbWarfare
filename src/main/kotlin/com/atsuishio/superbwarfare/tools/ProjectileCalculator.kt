package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.tools.ProjectileCalculator.HORIZONTAL_STEP
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3
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

        val minY = level.minBuildHeight.toDouble()
        val maxY = level.maxBuildHeight.toDouble()
        if (startPos.y < minY) return fallbackToWorldBottom(startPos, level.minBuildHeight)
        val entryTime = if (startPos.y >= maxY) {
            descendingTimeAtHeight(startPos.y, vy, gravity, maxY) ?: return startPos
        } else {
            0.0
        }

        val horizontalSpeed = sqrt(vx * vx + vz * vz)

        // 纯垂直运动（无水平位移）：直接检测脚下地形
        if (horizontalSpeed < 1e-6) {
            return calculateVerticalImpact(level, startPos)
        }

        // 水平步进的时间增量：每前进 HORIZONTAL_STEP 方块水平距离所需的时间
        val dt = HORIZONTAL_STEP / horizontalSpeed
        var t = entryTime
        var prevX = startPos.x + vx * t
        var prevY = minOf(startPos.y, maxY)
        var prevZ = startPos.z + vz * t

        for (step in 0 until MAX_STEPS) {
            t += dt
            val x = startPos.x + vx * t
            // y(t) = y0 + vy*t + 0.5*g*t²  注意 gravity 为负值
            val y = startPos.y + vy * t + 0.5 * gravity * t * t
            val z = startPos.z + vz * t

            if (y > maxY) {
                val previous = Vec3(prevX, prevY, prevZ)
                val exit = positionAtHeight(previous, Vec3(x, y, z), maxY)
                raycastBlock(level, previous, exit)?.let { return it }
                val reentry = descendingTimeAtHeight(startPos.y, vy, gravity, maxY)
                    ?.takeIf { it > t } ?: return exit
                t = reentry
                prevX = startPos.x + vx * t
                prevY = maxY
                prevZ = startPos.z + vz * t
                continue
            }

            if (y < minY) {
                val previous = Vec3(prevX, prevY, prevZ)
                val exit = positionAtHeight(previous, Vec3(x, y, z), minY)
                return raycastBlock(level, previous, exit) ?: exit
            }

            val blockX = Mth.floor(x)
            val blockZ = Mth.floor(z)

            if (level.hasChunk(blockX shr 4, blockZ shr 4)) {
                // 判断弹丸当前位置能否看到天空：
                // - 能看到 → 室外开阔地带，高度图可靠，使用高度图优化
                // - 看不到 → 室内/遮挡环境，高度图显示的是天花板，回归射线检测
                val checkY = Mth.floor(y).coerceIn(level.minBuildHeight, level.maxBuildHeight - 1)
                if (level.canSeeSky(BlockPos(blockX, checkY, blockZ))) {
                    // 室外：通过高度图快速检测是否已抵达地形表面
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
                } else {
                    // 室内/遮挡：高度图显示的是天花板高度，回归原始的射线检测
                    raycastBlock(level, Vec3(prevX, prevY, prevZ), Vec3(x, y, z))?.let { return it }
                }
            }

            prevX = x
            prevY = y
            prevZ = z
        }

        // 超过最大步数，返回最后计算位置
        return Vec3(prevX, prevY, prevZ)
    }

    private fun calculateVerticalImpact(level: Level, startPos: Vec3): Vec3 {
        val blockX = Mth.floor(startPos.x)
        val blockZ = Mth.floor(startPos.z)
        val bottom = fallbackToWorldBottom(startPos, level.minBuildHeight)
        if (!level.hasChunk(blockX shr 4, blockZ shr 4)) return bottom

        val top = Vec3(startPos.x, minOf(startPos.y, level.maxBuildHeight.toDouble()), startPos.z)
        raycastBlock(level, top, bottom)?.let { return it }

        val checkY = Mth.floor(top.y).coerceIn(level.minBuildHeight, level.maxBuildHeight - 1)
        if (level.canSeeSky(BlockPos(blockX, checkY, blockZ))) {
            val terrainHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, blockX, blockZ).toDouble()
            return Vec3(startPos.x, minOf(terrainHeight, top.y), startPos.z)
        }
        return bottom
    }

    private fun descendingTimeAtHeight(startY: Double, verticalSpeed: Double, gravity: Double, height: Double): Double? {
        val offset = startY - height
        val time = if (gravity == 0.0) {
            if (verticalSpeed >= 0.0) return null
            -offset / verticalSpeed
        } else {
            val discriminant = verticalSpeed * verticalSpeed - 2.0 * gravity * offset
            if (discriminant < 0.0) return null
            val root = sqrt(discriminant)
            if (verticalSpeed < 0.0) 2.0 * offset / (root - verticalSpeed)
            else (-verticalSpeed - root) / gravity
        }
        return time.takeIf { it.isFinite() && it >= 0.0 }
    }

    private fun positionAtHeight(start: Vec3, end: Vec3, height: Double): Vec3 {
        val fraction = (height - start.y) / (end.y - start.y)
        return Vec3(Mth.lerp(fraction, start.x, end.x), height, Mth.lerp(fraction, start.z, end.z))
    }

    private fun raycastBlock(level: Level, start: Vec3, end: Vec3): Vec3? {
        val hit = level.clip(ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, null))
        return if (hit.type == HitResult.Type.BLOCK) hit.location else null
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
        val above = Vec3(current.x, minOf(terrainHeight + 2, level.maxBuildHeight.toDouble()), current.z)
        val below = Vec3(current.x, maxOf(terrainHeight - 2, level.minBuildHeight.toDouble()), current.z)
        raycastBlock(level, above, below)?.let { return it }

        // 策略2：沿弹道方向检测（处理地形垂直面）
        raycastBlock(level, prev, current)?.let { return it }

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
