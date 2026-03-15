package com.atsuishio.superbwarfare.tools

import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

object TrajectoryCalculator {
    /**
     * 计算击中目标所需的发射方向（单位向量）
     * @param start 发射起点（vec31）
     * @param target 目标点（vec32）
     * @param v 出膛速度标量（方块/刻）
     * @param g 重力加速度（正数，方块/刻²）
     * @return 包含发射方向的列表，按飞行时间排序[低伸, 高抛]
     */
    fun calculateShootVectors(start: Vec3, target: Vec3, v: Double, g: Double): MutableList<Vec3> {
        val directions = arrayListOf<Vec3>()

        // 计算位移
        val dx = target.x - start.x
        val dy = target.y - start.y
        val dz = target.z - start.z

        // 水平距离平方
        val dh2 = dx * dx + dz * dz

        // 二次方程系数：A*t^4 + B*t^2 + C = 0
        val pA = g * g
        val pB = 4.0 * (dy * g - v * v)
        val pC = 4.0 * (dh2 + dy * dy)

        // 计算判别式
        val discriminant = pB * pB - 4.0 * pA * pC

        if (discriminant < 0) {
            // 无解：初速度不足以到达目标
            return directions
        }

        val sqrtDisc = sqrt(discriminant)

        // 计算t^2的两个解
        val u1 = (-pB + sqrtDisc) / (2.0 * pA)
        val u2 = (-pB - sqrtDisc) / (2.0 * pA)

        // 收集有效的正解
        val validSolutions = arrayListOf<Double>()
        if (u1 > 1e-9) validSolutions.add(u1)
        if (u2 > 1e-9 && abs(u2 - u1) > 1e-9) validSolutions.add(u2)

        // 按飞行时间排序
        validSolutions.sortDescending()

        // 计算每个解对应的发射方向
        for (u in validSolutions) {
            val t = sqrt(u)

            // 计算方向分量
            val dirX = dx / (v * t)
            val dirZ = dz / (v * t)
            val dirY = (dy + 0.5 * g * u) / (v * t)

            // 归一化得到单位向量
            val direction = Vec3(dirX, dirY, dirZ).normalize()
            directions.add(direction)
        }

        return directions
    }

    /**
     * 获取低伸弹道发射向量（如果存在）
     */
    fun getFlatTrajectory(start: Vec3, target: Vec3, v: Double, g: Double): Vec3? {
        val trajectories = calculateShootVectors(start, target, v, g)
        return if (trajectories.isEmpty()) null else trajectories[0]
    }

    /**
     * 获取高抛弹道发射向量（如果存在）
     */
    fun getHighTrajectory(start: Vec3, target: Vec3, v: Double, g: Double): Vec3? {
        val trajectories = calculateShootVectors(start, target, v, g)
        return if (trajectories.size >= 2) trajectories[1] else if (trajectories.size == 1) trajectories[0] else null
    }

    @JvmStatic
    fun calculateLaunchVector(
        start: Vec3,
        target: Vec3,
        v: Double,
        g: Double,
        isDepressed: Boolean
    ): Vec3? {
        return if (isDepressed) getFlatTrajectory(start, target, v, g) else getHighTrajectory(start, target, v, g)
    }
}