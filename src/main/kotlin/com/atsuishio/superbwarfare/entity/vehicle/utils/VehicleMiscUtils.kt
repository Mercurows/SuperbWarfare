package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import org.joml.Math

/**
 * 处理载具杂项的工具类
 */
object VehicleMiscUtils {
    /**
     * 判断载具是否两栖
     * 
     * @param vehicle 载具
     * @return 是否两栖
     */
    @JvmStatic
    fun isAmphibious(vehicle: VehicleEntity): Boolean {
        val type = vehicle.vehicleType
        return type == VehicleType.TANK || type == VehicleType.APC || type == VehicleType.AA || type == VehicleType.CAR || type == VehicleType.BOAT
    }

    /**
     * 计算乘客下车时的偏移量
     * 
     * @param vehicle        载具
     * @param vehicleWidth   载具碰撞箱宽度
     * @param passengerWidth 乘客碰撞箱宽度
     * @return 偏移量
     */
    @JvmStatic
    fun getDismountOffset(vehicle: VehicleEntity, vehicleWidth: Double, passengerWidth: Double): Vec3 {
        val offset = (vehicleWidth + passengerWidth + 1.0E-5) / 1.75
        val yaw = vehicle.yRot + 90.0f
        val x = -Mth.sin(yaw * (Math.PI.toFloat() / 180))
        val z = Mth.cos(yaw * (Math.PI.toFloat() / 180))
        val n = Math.max(Math.abs(x), Math.abs(z))
        return Vec3(x.toDouble() * offset / n.toDouble(), 0.0, z.toDouble() * offset / n.toDouble())
    }
}
