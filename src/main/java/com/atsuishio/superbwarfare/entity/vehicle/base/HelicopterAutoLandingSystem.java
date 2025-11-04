package com.atsuishio.superbwarfare.entity.vehicle.base;

import com.atsuishio.superbwarfare.init.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class HelicopterAutoLandingSystem {

    /**
     * 查找实体下方半球区域内最近的降落辅助方块位置
     *
     * @param radius 搜索半径
     * @return 辅助方块顶面位置，如果未找到则返回null
     */
    public static Vec3 findNearestLandingPos(VehicleEntity entity, int radius) {
        Level world = entity.level();
        BlockPos entityPos = entity.blockPosition();
        List<BlockPos> landingBlocks = new ArrayList<>();

        // 遍历半球区域内的所有方块
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= 0; y++) { // 只检查实体下方的区域
                    // 检查是否在半球内 (x² + y² + z² ≤ r²)
                    if (x * x + y * y + z * z <= radius * radius) {
                        BlockPos checkPos = entityPos.offset(x, y, z);

                        // 检查是否为降落辅助方块
                        if (world.getBlockState(checkPos).is(ModTags.Blocks.AUTO_LANDING)) {
                            landingBlocks.add(checkPos);
                        }
                    }
                }
            }
        }

        // 如果没有找到降落辅助方块，返回null
        if (landingBlocks.isEmpty()) {
            return null;
        }

        // 按距离排序，找到最近的降落辅助方块
        landingBlocks.sort(Comparator.comparingDouble(pos ->
                entity.position().distanceToSqr(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5)));

        return landingBlocks.get(0).getCenter();
    }
    public static void updateAutoLanding(VehicleEntity entity, Vec3 landingTarget) {
        // 计算水平方向上的偏移向量 (忽略Y轴)
        Vec3 currentPos = entity.position();
        Vec3 horizontalOffset = new Vec3(
                landingTarget.x - currentPos.x,
                0,
                landingTarget.z - currentPos.z
        );

        entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.975, 0.99, 0.975));

        // 计算距离和方向
        double horizontalDistance = horizontalOffset.length();
        Vec3 horizontalDirection = horizontalDistance > 0 ?
                horizontalOffset.normalize() : Vec3.ZERO;


        // 倾斜平滑因子
        float tiltSmoothingFactor = 0.1f;

        double horizontalDistanceNew = horizontalDistance - 5 * entity.getDeltaMovement().horizontalDistance();

        // 计算需要的倾斜角度 (与距离成正比，但有最大限制)
        // 直升机辅助降落这一块
        // 最大倾斜角度(度)
        float maxTiltAngle = 15.0f;
        float targetTilt = (float) Math.min(maxTiltAngle, horizontalDistanceNew * 2);

        // 将世界方向转换为本地倾斜方向
        // 需要考虑直升机的当前偏航角(yRot)
        float yawRad = Math.toRadians(-entity.getYRot());
        Vec3 localDirection = new Vec3(
                horizontalDirection.x * Math.cos(yawRad) - horizontalDirection.z * Math.sin(yawRad),
                0,
                horizontalDirection.x * Math.sin(yawRad) + horizontalDirection.z * Math.cos(yawRad)
        );

        // 计算目标俯仰和滚转
        float targetXRot = (float) (-localDirection.z * targetTilt);
        float targetZRot = (float) (localDirection.x * targetTilt);

        // 平滑过渡到目标姿态
        entity.setXRot(lerpAngle(entity.getXRot(), -targetXRot, tiltSmoothingFactor));
        entity.setZRot(lerpAngle(entity.getRoll(), -targetZRot, tiltSmoothingFactor));
    }

    // 角度线性插值方法
    private static float lerpAngle(float current, float target, float factor) {
        // 处理角度环绕
        float diff = target - current;
        while (diff < -180) diff += 360;
        while (diff > 180) diff -= 360;

        return current + diff * factor;
    }
}
