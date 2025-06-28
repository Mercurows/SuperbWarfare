package com.atsuishio.superbwarfare.tools;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

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

    public static Vec3 calculateLaunchVector(Vec3 pos, Vec3 pos2, double velocity, double g, boolean isDepressed) {
        double dx = pos2.x - pos.x;
        double dy = pos2.y - pos.y;
        double dz = pos2.z - pos.z;
        double horizontalDistSq = dx * dx + dz * dz;

        double a = 0.25 * g * g;
        double b = -velocity * velocity - g * dy;
        double c = horizontalDistSq + dy * dy;

        List<Double> validT = getDoubles(b, a, c);

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
            throw new IllegalStateException("No valid trajectory: Increase velocity or adjust target");
        }

        double sqrtDisc = Math.sqrt(discriminant);
        double u1 = (-b + sqrtDisc) / (2 * a);
        double u2 = (-b - sqrtDisc) / (2 * a);

        List<Double> validT = new ArrayList<>();
        if (u1 > 0) validT.add(Math.sqrt(u1));
        if (u2 > 0) validT.add(Math.sqrt(u2));

        if (validT.isEmpty()) {
            throw new IllegalStateException("No positive real solution for flight time");
        }
        return validT;
    }

}
