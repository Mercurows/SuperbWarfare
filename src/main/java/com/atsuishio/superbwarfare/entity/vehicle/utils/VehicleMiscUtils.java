package com.atsuishio.superbwarfare.entity.vehicle.utils;

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Math;

/**
 * 处理载具杂项的工具类
 */
public final class VehicleMiscUtils {

    /**
     * 判断载具是否两栖
     *
     * @param vehicle 载具
     * @return 是否两栖
     */
    public static boolean isAmphibious(VehicleEntity vehicle) {
        var type = vehicle.getVehicleType();
        return type == VehicleType.TANK
                || type == VehicleType.APC
                || type == VehicleType.AA
                || type == VehicleType.CAR
                || type == VehicleType.BOAT;
    }

    /**
     * 计算乘客下车时的偏移量
     *
     * @param vehicle        载具
     * @param vehicleWidth   载具碰撞箱宽度
     * @param passengerWidth 乘客碰撞箱宽度
     * @return 偏移量
     */
    public static Vec3 getDismountOffset(VehicleEntity vehicle, double vehicleWidth, double passengerWidth) {
        double offset = (vehicleWidth + passengerWidth + (double) 1.0E-5f) / 1.75;
        float yaw = vehicle.getYRot() + 90.0f;
        float x = -Mth.sin(yaw * ((float) Math.PI / 180));
        float z = Mth.cos(yaw * ((float) Math.PI / 180));
        float n = Math.max(Math.abs(x), Math.abs(z));
        return new Vec3((double) x * offset / (double) n, 0, (double) z * offset / (double) n);
    }
}
