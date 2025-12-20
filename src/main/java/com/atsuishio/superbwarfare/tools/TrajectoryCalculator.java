package com.atsuishio.superbwarfare.tools;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class TrajectoryCalculator {
    /**
     * 计算击中目标所需的发射方向（单位向量）
     * @param start 发射起点（vec31）
     * @param target 目标点（vec32）
     * @param v 出膛速度标量（方块/刻）
     * @param g 重力加速度（正数，方块/刻²）
     * @return 包含发射方向的列表，按飞行时间排序[低伸, 高抛]
     */
    public static List<Vec3> calculateShootVectors(Vec3 start, Vec3 target, double v, double g) {
        List<Vec3> directions = new ArrayList<>();
        
        // 计算位移
        double dx = target.x - start.x;
        double dy = target.y - start.y;
        double dz = target.z - start.z;
        
        // 水平距离平方
        double dh2 = dx * dx + dz * dz;
        
        // 二次方程系数：A*t^4 + B*t^2 + C = 0
        double A = g * g;
        double B = 4.0 * (dy * g - v * v);
        double C = 4.0 * (dh2 + dy * dy);
        
        // 计算判别式
        double discriminant = B * B - 4.0 * A * C;
        
        if (discriminant < 0) {
            // 无解：初速度不足以到达目标
            return directions;
        }
        
        double sqrtDisc = Math.sqrt(discriminant);
        
        // 计算t^2的两个解
        double u1 = (-B + sqrtDisc) / (2.0 * A);
        double u2 = (-B - sqrtDisc) / (2.0 * A);
        
        // 收集有效的正解
        List<Double> validSolutions = new ArrayList<>();
        if (u1 > 1e-9) validSolutions.add(u1);
        if (u2 > 1e-9 && Math.abs(u2 - u1) > 1e-9) validSolutions.add(u2);
        
        // 按飞行时间排序（t = sqrt(u)）
        validSolutions.sort((a, b) -> Double.compare(Math.sqrt(a), Math.sqrt(b)));
        
        // 计算每个解对应的发射方向
        for (double u : validSolutions) {
            double t = Math.sqrt(u);
            
            // 计算方向分量
            double dirX = dx / (v * t);
            double dirZ = dz / (v * t);
            double dirY = (dy + 0.5 * g * u) / (v * t);
            
            // 归一化得到单位向量
            Vec3 direction = new Vec3(dirX, dirY, dirZ).normalize();
            directions.add(direction);
        }
        
        return directions;
    }
    
    /**
     * 获取低伸弹道发射向量（如果存在）
     */
    public static Vec3 getFlatTrajectory(Vec3 start, Vec3 target, double v, double g) {
        List<Vec3> trajectories = calculateShootVectors(start, target, v, g);
        return trajectories.isEmpty() ? null : trajectories.get(0);
    }
    
    /**
     * 获取高抛弹道发射向量（如果存在）
     */
    public static Vec3 getHighTrajectory(Vec3 start, Vec3 target, double v, double g) {
        List<Vec3> trajectories = calculateShootVectors(start, target, v, g);
        return trajectories.size() >= 2 ? trajectories.get(1) : 
               trajectories.size() == 1 ? trajectories.get(0) : null;
    }

    public static Vec3 calculateLaunchVector(Vec3 start, Vec3 target, double v, double g, boolean isDepressed) {
        return isDepressed ? getFlatTrajectory(start, target, v, g) : getHighTrajectory(start, target, v, g);
    }
}