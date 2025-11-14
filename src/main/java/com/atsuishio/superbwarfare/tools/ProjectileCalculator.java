package com.atsuishio.superbwarfare.tools;

import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.Optional;

public class ProjectileCalculator {
    private static final double TIME_STEP = 0.05; // 时间步长（刻）
    private static final int MAX_ITERATIONS = 2000; // 最大迭代次数
    private static final double COLLISION_THRESHOLD = 0.001; // 碰撞检测阈值

    /**
     * 计算炮弹精确落点位置（Vec3）
     *
     * @param level        世界对象
     * @param startPos     发射点位置（Vec3）
     * @param launchVector 发射向量（Vec3）
     * @return 精确的落点位置（Vec3），如果没有碰撞则返回最后位置
     */
    public static Vec3 calculatePreciseImpactPoint(Level level, Vec3 startPos, Vec3 launchVector, double velocity, double gravity) {
        Vec3 currentPos = startPos;
        Vec3 currentVelocity = launchVector.normalize().scale(velocity);
        Vec3 previousPos = startPos;

        for (int i = 0; i < MAX_ITERATIONS; i++) {
            // 计算下一个位置
            Vec3 nextPos = currentPos.add(
                    currentVelocity.x * TIME_STEP,
                    currentVelocity.y * TIME_STEP,
                    currentVelocity.z * TIME_STEP
            );

            // 应用重力
            currentVelocity = currentVelocity.add(0, gravity * TIME_STEP, 0);

            // 检查碰撞
            Optional<Vec3> collisionPoint = checkCollision(level, previousPos, nextPos);

            if (collisionPoint.isPresent()) {
                // 精确计算碰撞点
                return refineCollisionPoint(level, previousPos, collisionPoint.get());
            }

            // 边界检查
            if (nextPos.y < level.getMinBuildHeight()) {
                return new Vec3(nextPos.x, level.getMinBuildHeight(), nextPos.z);
            }

            // 更新位置
            previousPos = currentPos;
            currentPos = nextPos;
        }

        // 超过最大迭代次数，返回最后位置
        return currentPos;
    }

    /**
     * 检查两点之间是否有碰撞
     */
    private static Optional<Vec3> checkCollision(Level level, Vec3 start, Vec3 end) {
        // 使用Minecraft内置的光线追踪进行碰撞检测
        BlockHitResult hitResult = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER, // 只检测碰撞方块
                ClipContext.Fluid.NONE, // 忽略流体
                CollisionContext.empty() // 无实体
        ));

        // 如果检测到碰撞，返回碰撞点
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return Optional.of(hitResult.getLocation());
        }

        // 没有检测到碰撞
        return Optional.empty();
    }

    /**
     * 精确计算碰撞点（使用二分法提高精度）
     */
    private static Vec3 refineCollisionPoint(Level level, Vec3 safePoint, Vec3 collisionPoint) {
        Vec3 low = safePoint;
        Vec3 high = collisionPoint;
        Vec3 bestPoint = collisionPoint;

        // 二分法迭代提高精度
        for (int i = 0; i < 10; i++) {
            Vec3 mid = low.add(high.subtract(low).scale(0.5));

            // 检查从安全点到中点是否有碰撞
            Optional<Vec3> collision = checkCollision(level, low, mid);

            if (collision.isPresent()) {
                // 有碰撞，将高点移动到中点
                high = mid;
                bestPoint = collision.get();
            } else {
                // 无碰撞，将低点移动到中点
                low = mid;
            }
        }

        return bestPoint;
    }
}
