package com.atsuishio.superbwarfare.tools;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class ProjectileCalculator {
    private static final double TIME_STEP = 0.1; // 时间步长（刻）
    private static final int MAX_ITERATIONS = 1000; // 最大迭代次数

    /**
     * 计算炮弹落地位置
     *
     * @param world 世界对象
     * @param startPos 发射点位置
     * @param launchVector 发射向量
     * @return 预测的落点方块位置
     */
    public static BlockPos calculateImpactPosition(Level world, Vec3 startPos, Vec3 launchVector, double gravity) {
        // 当前炮弹位置和速度
        Vec3 currentPos = startPos;
        Vec3 currentVelocity = launchVector;

        // 记录上一刻位置
        Vec3 prevPos = startPos;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // 更新位置
            Vec3 nextPos = currentPos.add(
                    currentVelocity.x * TIME_STEP,
                    currentVelocity.y * TIME_STEP,
                    currentVelocity.z * TIME_STEP
            );

            // 更新速度（重力影响）
            currentVelocity = currentVelocity.add(0, gravity * TIME_STEP, 0);

            // 检查是否碰撞方块
            BlockPos collisionPos = checkCollision(world, prevPos, nextPos);
            if (collisionPos != null) {
                return collisionPos;
            }

            // 更新位置进行下一步
            prevPos = currentPos;
            currentPos = nextPos;

            // 安全检查：防止飞出世界边界
            if (currentPos.y < world.getMinBuildHeight() || currentPos.y > world.getMaxBuildHeight()) {
                return new BlockPos(
                        (int)Math.floor(currentPos.x),
                        (int)Math.floor(currentPos.y),
                        (int)Math.floor(currentPos.z)
                );
            }
        }

        // 超过最大迭代次数，返回当前位置
        return new BlockPos(
                (int)Math.floor(currentPos.x),
                (int)Math.floor(currentPos.y),
                (int)Math.floor(currentPos.z)
        );
    }

    /**
     * 检查两点之间是否有碰撞方块
     */
    private static BlockPos checkCollision(Level world, Vec3 start, Vec3 end) {
        // 使用距离和方向向量
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;
        double distance = Math.sqrt(dx*dx + dy*dy + dz*dz);

        if (distance == 0) return null;

        // 方向单位向量
        double dirX = dx / distance;
        double dirY = dy / distance;
        double dirZ = dz / distance;

        // 步进检查
        double stepSize = 0.1; // 检查步长
        for (double t = 0; t < distance; t += stepSize) {
            double x = start.x + dirX * t;
            double y = start.y + dirY * t;
            double z = start.z + dirZ * t;

            BlockPos pos = new BlockPos((int)Math.floor(x), (int)Math.floor(y), (int)Math.floor(z));
            BlockState state = world.getBlockState(pos);

            // 检查是否碰到固体方块
            if (!state.isAir()) {
                return pos;
            }

            // 检查是否碰到下方方块（炮弹落地）
            BlockPos belowPos = pos.below();
            BlockState belowState = world.getBlockState(belowPos);

            if (y - Math.floor(y) < 0.1 && !belowState.isAir()) {
                return belowPos;
            }
        }

        return null;
    }


    /**
     * 快速预测落点（不考虑地形，仅数学计算）
     * 用于平坦地形或初始估算
     */
    public static Vec3 estimateLandingPosition(Vec3 startPos, Vec3 launchVector, double gravity) {
        double vx = launchVector.x;
        double vy = launchVector.y;
        double vz = launchVector.z;

        // 计算飞行时间 (解二次方程: y = y0 + vy*t + 0.5*g*t² = 0)
        double a = 0.5 * gravity;
        double b = vy;
        double c = startPos.y; // 假设地面高度为0

        // 计算判别式
        double discriminant = b*b - 4*a*c;

        if (discriminant < 0) {
            // 无实数解，炮弹不会落地
            return null;
        }

        // 取正数解
        double t = (-b + Math.sqrt(discriminant)) / (2*a);
        if (t < 0) {
            t = (-b - Math.sqrt(discriminant)) / (2*a);
        }

        // 计算落点
        double x = startPos.x + vx * t;
        double z = startPos.z + vz * t;

        return new Vec3(x, 0, z);
    }
}
