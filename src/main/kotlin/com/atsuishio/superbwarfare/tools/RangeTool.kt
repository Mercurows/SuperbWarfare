package com.atsuishio.superbwarfare.tools

import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RangeTool {
    /**
     * 计算迫击炮理论水平射程
     *
     * @param thetaDegrees 发射角度（以度为单位），需要根据实际情况修改
     * @param v            初始速度
     * @param g            重力加速度
     */
    @JvmStatic
    fun getRange(thetaDegrees: Double, v: Double, g: Double): Double {
        val t = v * sin(thetaDegrees * Mth.DEG_TO_RAD) / g * 2
        return t * v * cos(thetaDegrees * Mth.DEG_TO_RAD)
    }

    private const val TOLERANCE = 1e-3 // 牛顿迭代法的容差
    private const val MAX_ITERATIONS = 50 // 最大迭代次数

    /**
     * 计算炮弹发射向量
     *
     * @param launchPos      炮弹发射位置 (Vec3)
     * @param targetPos      目标当前位置 (Vec3)
     * @param targetVel      目标速度向量 (Vec3，单位：方块/tick)
     * @param muzzleVelocity 炮弹出膛速度 (标量，单位：方块/tick)
     * @return 炮弹的发射向量 (Vec3)，若无法击中则返回预测值
     */
    @JvmStatic
    fun calculateFiringSolution(
        launchPos: Vec3,
        targetPos: Vec3,
        targetVel: Vec3,
        muzzleVelocity: Double,
        gravity: Double
    ): Vec3 {
        val d0 = targetPos.subtract(launchPos) // 位置差向量
        val dSqr = d0.lengthSqr() // |d0|²
        val dot = d0.dot(targetVel) // d0 · u
        val absSqr = targetVel.lengthSqr() // |u|²

        // 计算四次方程的系数
        val a = 0.25 * gravity * gravity
        val b = gravity * targetVel.y
        val c = absSqr + gravity * d0.y - muzzleVelocity * muzzleVelocity
        val d = 2 * dot

        // 牛顿迭代法求解时间 t
        var t = estimateInitialTime(d0, muzzleVelocity) // 初始估计值
        var prevT = t

        for (i in 0..<MAX_ITERATIONS) {
            val t2 = t * t
            val t3 = t2 * t
            val t4 = t3 * t

            // 计算函数值 f(t) = a*t⁴ + b*t³ + c*t² + d*t + e
            val f = a * t4 + b * t3 + c * t2 + d * t + dSqr
            if (abs(f) < TOLERANCE) break

            // 计算导数值 f'(t) = 4a*t³ + 3b*t² + 2c*t + d
            val df = 4 * a * t3 + 3 * b * t2 + 2 * c * t + d
            if (abs(df) < 1e-10) {
                t = prevT + 0.1 // 避免除零，调整t
                continue
            }

            prevT = t
            t -= f / df // 牛顿迭代

            // 确保t为正数
            if (t < 0) t = 0.1
        }

        // 检查解的有效性
        if (t > 0) {
            val invT = 1 / t
            // 计算速度分量
            val vx = d0.x * invT + targetVel.x
            val vz = d0.z * invT + targetVel.z
            val vy = d0.y * invT + targetVel.y + 0.5 * gravity * t
            return Vec3(vx, vy, vz)
        } else {
            // 备选方案：线性预测目标位置
            val fallbackT = sqrt(dSqr) / muzzleVelocity
            val predictedPos = targetPos.add(targetVel.scale(fallbackT))
            val toPredicted = predictedPos.subtract(launchPos)
            val vy = (toPredicted.y + 0.5 * gravity * fallbackT * fallbackT) / fallbackT
            val horizontal = Vec3(toPredicted.x, 0.0, toPredicted.z).normalize()
            val horizontalSpeed = sqrt(muzzleVelocity * muzzleVelocity - vy * vy)
            return Vec3(
                horizontal.x * horizontalSpeed,
                vy,
                horizontal.z * horizontalSpeed
            )
        }
    }

    // 初始时间估计（无重力无移动的飞行时间）
    private fun estimateInitialTime(d0: Vec3, velocity: Double) = d0.length() / velocity
}
