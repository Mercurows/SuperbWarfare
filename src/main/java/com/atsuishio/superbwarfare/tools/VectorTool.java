package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public class VectorTool {
    public static double calculateAngle(Vec3 start, Vec3 end) {
        double startLength = start.length();
        double endLength = end.length();
        if (startLength > 0.0D && endLength > 0.0D) {
            return Math.toDegrees(Math.acos(Mth.clamp(start.dot(end) / (startLength * endLength), -1, 1)));
        } else {
            return 0.0D;
        }
    }

    public static float calculateY(float x) {
        if (x < -90) {
            return (-(x + 180.0f) / 90.0f);  // x ∈ [-180, -90)
        } else if (x <= 90) {
            return (x / 90.0f);              // x ∈ [-90, 90]
        } else {
            return ((180.0f - x) / 90.0f);   // x ∈ (90, 180]
        }
    }

    // 合并三个旋转（Yaw -> Pitch -> Roll）
    public static Quaternionf combineRotations(float partialTicks, VehicleEntity entity) {
        // 1. 获取三个独立的旋转四元数
        Quaternionf yawRot = Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()));
        Quaternionf pitchRot = Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()));
        Quaternionf rollRot = Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.prevRoll, entity.getRoll()));

        // 2. 按照正确顺序合并：先Yaw，再Pitch，最后Roll
        Quaternionf combined = new Quaternionf(yawRot);   // 初始化为Yaw旋转
        combined.mul(pitchRot);  // 应用Pitch旋转
        combined.mul(rollRot);   // 应用Roll旋转

        return combined;
    }

    // 仅水平旋转
    public static Quaternionf combineRotationsYaw(float partialTicks, VehicleEntity entity) {
        Quaternionf yawRot = Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()));
        return new Quaternionf(yawRot);
    }

    public static Quaternionf combineRotationsTurret(float partialTicks, VehicleEntity entity) {
        Quaternionf turretYawRot = Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.turretYRotO, entity.getTurretYRot()));
        Quaternionf combined = combineRotations(partialTicks, entity);
        combined.mul(turretYawRot);

        return combined;
    }

    public static Quaternionf combineRotationsBarrel(float partialTicks, VehicleEntity entity) {
        Quaternionf turretPitchRot = Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.turretXRotO, entity.getTurretXRot()));
        Quaternionf combined = combineRotationsTurret(partialTicks, entity);
        combined.mul(turretPitchRot);

        return combined;
    }

    public static Vec3 randomPos(Vec3 originPos, int radius) {
        return originPos.add(new Vec3(Math.random() * radius, 0, 0).yRot((float) (360 * Math.random()) * Mth.DEG_TO_RAD));
    }

    public static boolean isInLiquid(Level level, Vec3 position) {
        // 将 Vec3 转换为 BlockPos（获取所在方块位置）
        BlockPos blockPos = BlockPos.containing(position);

        // 获取该位置的流体状态
        FluidState fluidState = level.getFluidState(blockPos);

        // 检查流体是否有效且位置低于流体表面
        if (!fluidState.isEmpty()) {
            // 获取流体在方块中的高度（0.0 - 1.0）
            float fluidHeight = fluidState.getHeight(level, blockPos);
            // 计算位置相对于当前方块底部的偏移量
            double yOffset = position.y - blockPos.getY();
            // 如果位置低于流体表面则返回 true
            return yOffset < fluidHeight;
        }
        return false;
    }
}
