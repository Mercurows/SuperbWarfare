package com.atsuishio.superbwarfare.tools;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.mojang.math.Axis;
import net.minecraft.util.Mth;
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
}
