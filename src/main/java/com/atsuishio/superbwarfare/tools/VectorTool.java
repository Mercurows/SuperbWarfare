package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.mojang.math.Axis;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaterniond;
import org.joml.Quaternionf;

public class VectorTool {
    public static double calculateAngle(Vec3 start, Vec3 end) {
        double startLength = start.length();
        double endLength = end.length();
        if (startLength > 0 && endLength > 0) {
            return Math.toDegrees(Math.acos(Mth.clamp(start.dot(end) / (startLength * endLength), -1, 1)));
        } else {
            return 0;
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
    public static Quaterniond combineRotations(float partialTicks, VehicleEntity entity) {
        // 1. 获取三个独立的旋转四元数
        Quaternionf yawRot = Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()));
        Quaternionf pitchRot = Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.getXRot()));
        Quaternionf rollRot = Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.prevRoll, entity.getRoll()));

        // 2. 按照正确顺序合并：先Yaw，再Pitch，最后Roll
        Quaterniond combined = new Quaterniond(yawRot);   // 初始化为Yaw旋转
        combined.mul(new Quaterniond(pitchRot));  // 应用Pitch旋转
        combined.mul(new Quaterniond(rollRot));   // 应用Roll旋转

        return combined;
    }

    // 仅水平旋转
    public static Quaterniond combineRotationsYaw(float partialTicks, VehicleEntity entity) {
        Quaternionf yawRot = Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entity.yRotO, entity.getYRot()));
        return new Quaterniond(yawRot);
    }

    public static Quaterniond combineRotationsTurret(float partialTicks, VehicleEntity entity) {
        Quaternionf turretYawRot = Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.turretYRotO, entity.getTurretYRot()));
        Quaterniond combined = combineRotations(partialTicks, entity);
        combined.mul(new Quaterniond(turretYawRot));

        return combined;
    }

    public static Quaterniond combineRotationsBarrel(float partialTicks, VehicleEntity entity) {
        Quaternionf turretPitchRot = Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.turretXRotO, entity.getTurretXRot()));
        Quaterniond combined = combineRotationsTurret(partialTicks, entity);
        combined.mul(new Quaterniond(turretPitchRot));

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
            // 获取流体在方块中的高度（0 - 1）
            float fluidHeight = fluidState.getHeight(level, blockPos);
            // 计算位置相对于当前方块底部的偏移量
            double yOffset = position.y - blockPos.getY();
            // 如果位置低于流体表面则返回 true
            return yOffset < fluidHeight;
        }
        return false;
    }

    /**
     * 计算镜面反射向量。
     *
     * @param v1 入射向量（弹射物的方向向量，如运动向量）。
     * @param v0 平面法向量（朝向向量）。
     * @return 反射向量 v2。
     */
    public static Vec3 calculateReflection(Vec3 v1, Vec3 v0) {
        // 归一化法向量（确保单位长度）

        // 计算点积 v1 · n
        double dot = v1.dot(v0);

        // 计算反射向量: v2 = v1 - 2 * (v1 · n) * n

        return v1.subtract(v0.scale(2 * dot));
    }

    public static Vec3 lerpGetEntityBoundingBoxCenter(Entity entity, float partialTick) {
        return new Vec3(Mth.lerp(partialTick, entity.xo, entity.getX()), Mth.lerp(partialTick, entity.yo + entity.getBbHeight() / 2, entity.getY() + entity.getBbHeight() / 2), Mth.lerp(partialTick, entity.zo, entity.getZ()));
    }

    public static boolean checkNoClip(Vec3 pos1, Vec3 pos2, Level level) {
        return level.clip(new ClipContext(pos1, pos2,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, (Entity) null)).getType() != HitResult.Type.BLOCK;
    }
}
