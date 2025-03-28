package com.atsuishio.superbwarfare.client.gui;

import net.minecraft.world.phys.Vec3;

public class RangeHelper {

    public static final int MAX_RANGE = 1145;

    /**
     * 计算迫击炮理论水平射程
     *
     * @param thetaDegrees 发射角度（以度为单位），需要根据实际情况修改
     */
    public static double getRange(double thetaDegrees) {
        double initialVelocity = 11.4; // 初始速度 11.4 m/s
        double thetaRadians = Math.toRadians(thetaDegrees); // 将角度转换为弧度
        double gravity = 0.146; // 重力加速度
        double velocityDecay = 0.99; // 速度衰减系数

        // 计算射程
        return calculateRange(initialVelocity, thetaRadians, gravity, velocityDecay);
    }

    public static double calculateRange(double initialVelocity, double theta, double gravity, double velocityDecay) {
        double vx = initialVelocity * Math.cos(theta); // 水平速度
        double vy = initialVelocity * Math.sin(theta); // 垂直速度

        double x = 0.0; // 水平位置
        double y = 1.0; // 垂直位置

        // 当炮弹还未触地时，继续计算飞行轨迹
        while (y >= 0) {
            // 更新位置
            x += vx;
            y += vy;

            // 更新速度
            vx *= velocityDecay;
            vy = vy * velocityDecay - gravity;

            // 如果炮弹触地，则跳出循环
            if (y < 0) {
                break;
            }
        }

        // 返回最终水平距离
        return x;
    }

    public static double calculateRangeWithDeltaY(double initialVelocity, double theta, double gravity, double velocityDecay, double deltaY) {
        double vx = initialVelocity * Math.cos(theta); // 水平速度
        double vy = initialVelocity * Math.sin(theta); // 垂直速度

        double range = 0.0; // 水平距离
        double y = 1.0; // 垂直位置

        double commonRange = calculateRange(initialVelocity, theta, gravity, velocityDecay);

        // 当炮弹还未触地时，继续计算飞行轨迹
        while (range < commonRange / 2 || (range >= commonRange / 2 && y >= deltaY)) {
            // 更新位置
            range += vx;
            y += vy;

            // 更新速度
            vx *= velocityDecay;
            vy = vy * velocityDecay - gravity;

            if (range >= commonRange / 2 && y < deltaY) {
                break;
            }
        }

        // 返回最终水平距离
        return range;
    }

    public static boolean canReachTarget(double initialVelocity, double gravity, double velocityDecay, Vec3 startPos, Vec3 targetPos, double[] angles) {
        if (startPos.equals(targetPos)) {
            return false;
        }

        double startX = startPos.x;
        double startY = startPos.y;
        double startZ = startPos.z;

        double targetX = targetPos.x;
        double targetY = targetPos.y;
        double targetZ = targetPos.z;

        double distanceXZ = Math.sqrt(Math.pow(targetX - startX, 2) + Math.pow(targetZ - startZ, 2));
        if (distanceXZ > MAX_RANGE) {
            return false;
        }

        double theta = calculateLaunchAngle(initialVelocity, gravity, velocityDecay, distanceXZ, targetY - startY);

        if (theta < 20 || theta > 90) {
            return false;
        }

        angles[0] = Math.atan2(targetZ, targetX); // 水平角度
        angles[1] = theta; // 炮口抬升角度
        return true;
    }

    public static double calculateLaunchAngle(double initialVelocity, double gravity, double velocityDecay, double distanceXZ, double targetY) {
        double left = 20;  // 最小角度
        double right = 30; // 最大角度
        double tolerance = 0.5; // 允许的误差范围

        // 在 20 到 30 之间搜索
        while (right - left > tolerance) {
            double mid = (left + right) / 2;
            double radian = Math.toRadians(mid);
            double range = calculateRangeWithDeltaY(initialVelocity, radian, gravity, velocityDecay, targetY);

            if (Math.abs(range - distanceXZ) < tolerance * 8) {
                return mid;
            } else if (range < distanceXZ) {
                left = mid;
            } else {
                right = mid;
            }
        }

        // 如果在 20 到 30 之间没有找到合适的角度，则在 30 到 90 之间搜索
        left = 30;
        right = 90;

        while (right - left > tolerance) {
            double mid = (left + right) / 2;
            double radian = Math.toRadians(mid);
            double range = calculateRangeWithDeltaY(initialVelocity, radian, gravity, velocityDecay, targetY);

            if (Math.abs(range - distanceXZ) < tolerance * 8) {
                return mid;
            } else if (range < distanceXZ) {
                right = mid;
            } else {
                left = mid;
            }
        }

        // 如果仍然没有找到合适的角度，则返回 -1
        return -1;
    }
}
