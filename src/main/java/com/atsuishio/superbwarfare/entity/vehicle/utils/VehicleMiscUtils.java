package com.atsuishio.superbwarfare.entity.vehicle.utils;

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;

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
}
