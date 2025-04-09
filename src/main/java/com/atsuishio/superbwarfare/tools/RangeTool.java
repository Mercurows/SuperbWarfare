package com.atsuishio.superbwarfare.tools;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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

    /**
     * 判断按指定参数发射是否可以击中目标
     *
     * @param v                     初始速度
     * @param g                     重力加速度
     * @param startPos              起始位置
     * @param endPos                目标位置
     * @param minAngle              最小仰角
     * @param maxAngle              最大仰角
     * @param isDepressedTrajectory 是否使用低伸弹道
     */
    public static boolean canReach(double v, double g, Vec3 startPos, Vec3 endPos, double minAngle, double maxAngle, boolean isDepressedTrajectory) {
        if (getD(v, g, startPos, endPos) < 0) return false;

        var targetAngle = calculateAngle(v, g, startPos, endPos, isDepressedTrajectory);
        return targetAngle >= minAngle && targetAngle <= maxAngle;
    }

    /**
     * 计算按指定参数发射所需的仰角
     *
     * @param v                     初始速度
     * @param g                     重力加速度
     * @param startPos              起始位置
     * @param endPos                目标位置
     * @param isDepressedTrajectory 是否使用低伸弹道
     */
    public static double calculateAngle(double v, double g, Vec3 startPos, Vec3 endPos, boolean isDepressedTrajectory) {
        var xDiff = startPos.x - endPos.x;
        var zDiff = startPos.z - endPos.z;
        var x = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(zDiff, 2));
        double d = getD(v, g, startPos, endPos);
        return Math.atan((v * v + (isDepressedTrajectory ? -d : d)) / (g * x)) * Mth.RAD_TO_DEG;
    }

    private static double getD(double v, double g, Vec3 startPos, Vec3 endPos) {
        var xDiff = startPos.x - endPos.x;
        var zDiff = startPos.z - endPos.z;
        var x = Math.sqrt(Math.pow(xDiff, 2) + Math.pow(zDiff, 2));
        var y = startPos.y - endPos.y;

        return Math.sqrt(Math.pow(v, 4) - g * g * x * x - 2 * g * y * v * v);
    }

}
