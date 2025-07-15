package com.atsuishio.superbwarfare.tools;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RangeTool {

    /**
     * 计算迫击炮理论水平射程
     *
     * @param thetaDegrees 发射角度（以度为单位），需要根据实际情况修改
     * @param v            初始速度
     * @param g            重力加速度
     */
    public static double getRange(double thetaDegrees, double v, double g) {
        double t = v * Math.sin(thetaDegrees * Mth.DEG_TO_RAD) / g * 2;
        return t * v * Math.cos(thetaDegrees * Mth.DEG_TO_RAD);
    }

    // 谢谢DeepSeek
    @Nullable
    public static Vec3 calculateLaunchVector(Vec3 pos, Vec3 pos2, double velocity, double gravity, boolean isDepressed) {
        double dx = pos2.x - pos.x;
        double dy = pos2.y - pos.y;
        double dz = pos2.z - pos.z;
        double horizontalDistSq = dx * dx + dz * dz;

        double g = -gravity;

        double a = 0.25 * g * g;
        double b = -velocity * velocity - g * dy;
        double c = horizontalDistSq + dy * dy;

        List<Double> validT = getDoubles(b, a, c);
        if (validT.isEmpty()) return null;

        double t;

        if (isDepressed) {
            t = Collections.min(validT);
        } else {
            t = Collections.max(validT);
        }

        double vx = dx / t;
        double vz = dz / t;
        double vy = (dy - 0.5 * g * t * t) / t;

        return new Vec3(vx, vy, vz);
    }

    private static List<Double> getDoubles(double b, double a, double c) {
        double discriminant = b * b - 4 * a * c;
        if (discriminant < 0) {
            return List.of();
        }

        double sqrtDisc = Math.sqrt(discriminant);
        double u1 = (-b + sqrtDisc) / (2 * a);
        double u2 = (-b - sqrtDisc) / (2 * a);

        List<Double> validT = new ArrayList<>();
        if (u1 > 0) validT.add(Math.sqrt(u1));
        if (u2 > 0) validT.add(Math.sqrt(u2));

        return validT;
    }

    private static final double TOLERANCE = 1e-3; // 牛顿迭代法的容差
    private static final int MAX_ITERATIONS = 50; // 最大迭代次数

    /**
     * 计算炮弹发射向量
     *
     * @param launchPos      炮弹发射位置 (Vec3)
     * @param targetPos      目标当前位置 (Vec3)
     * @param targetVel      目标速度向量 (Vec3，单位：方块/tick)
     * @param muzzleVelocity 炮弹出膛速度 (标量，单位：方块/tick)
     * @return 炮弹的发射向量 (Vec3)，若无法击中则返回预测值
     */
    public static Vec3 calculateFiringSolution(Vec3 launchPos, Vec3 targetPos, Vec3 targetVel, double muzzleVelocity, double gravity) {
        Vec3 d0 = targetPos.subtract(launchPos); // 位置差向量
        double dSqr = d0.lengthSqr(); // |d0|²
        double dot = d0.dot(targetVel); // d0 · u
        double absSqr = targetVel.lengthSqr(); // |u|²

        // 计算四次方程的系数
        double a = 0.25 * gravity * gravity;
        double b = gravity * targetVel.y;
        double c = absSqr + gravity * d0.y - muzzleVelocity * muzzleVelocity;
        double d = 2 * dot;

        // 牛顿迭代法求解时间 t
        double t = estimateInitialTime(d0, muzzleVelocity); // 初始估计值
        double prevT = t;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            double t2 = t * t;
            double t3 = t2 * t;
            double t4 = t3 * t;

            // 计算函数值 f(t) = a*t⁴ + b*t³ + c*t² + d*t + e
            double f = a * t4 + b * t3 + c * t2 + d * t + dSqr;
            if (Math.abs(f) < TOLERANCE) break;

            // 计算导数值 f'(t) = 4a*t³ + 3b*t² + 2c*t + d
            double df = 4 * a * t3 + 3 * b * t2 + 2 * c * t + d;
            if (Math.abs(df) < 1e-10) {
                t = prevT + 0.1; // 避免除零，调整t
                continue;
            }

            prevT = t;
            t -= f / df; // 牛顿迭代

            // 确保t为正数
            if (t < 0) t = 0.1;
        }

        // 检查解的有效性
        if (t > 0) {
            double invT = 1.0 / t;
            // 计算速度分量
            double vx = d0.x * invT + targetVel.x;
            double vz = d0.z * invT + targetVel.z;
            double vy = d0.y * invT + targetVel.y + 0.5 * gravity * t;
            return new Vec3(vx, vy, vz);
        } else {
            // 备选方案：线性预测目标位置
            double fallbackT = Math.sqrt(dSqr) / muzzleVelocity;
            Vec3 predictedPos = targetPos.add(targetVel.scale(fallbackT));
            Vec3 toPredicted = predictedPos.subtract(launchPos);
            double vy = (toPredicted.y + 0.5 * gravity * fallbackT * fallbackT) / fallbackT;
            Vec3 horizontal = new Vec3(toPredicted.x, 0, toPredicted.z).normalize();
            double horizontalSpeed = Math.sqrt(muzzleVelocity * muzzleVelocity - vy * vy);
            return new Vec3(
                    horizontal.x * horizontalSpeed,
                    vy,
                    horizontal.z * horizontalSpeed
            );
        }
    }

    // 初始时间估计（无重力无移动的飞行时间）
    private static double estimateInitialTime(Vec3 d0, double velocity) {
        return d0.length() / velocity;
    }
}
