package com.atsuishio.superbwarfare.client.overlay.weapon

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * 载具 HUD 专用的姿态角工具。
 *
 * HUD 不能直接用 [VehicleEntity.getPitch] / [VehicleEntity.getRoll] / [VehicleEntity.getYaw]：
 * 这些是 [VehicleEntity.syncEulerFromBody] 为操控连续性维护的欧拉角，同一姿态有多组等价表示，
 * 倒飞/横滚后会停留在 pitch=180 之类的分支上，导致 HUD 仪表球采样和旋转错误（HUD 颠倒）。
 *
 * 这里统一从插值后的本体四元数（[VehicleEntity.getBodyRotation]）做规范化分解：
 * pitch 夹在 [-90, 90]，roll 由本体上/右向量相对世界水平面求得，yaw 由前向量水平投影求得。
 * 正常姿态下结果与原来的欧拉角完全一致，倒飞/横滚后也能得到与渲染朝向一致的数值。
 */
object VehicleHudQuat {
    /** HUD 用俯仰角（度，低头为正） */
    fun pitch(vehicle: VehicleEntity, partialTick: Float): Float {
        val f = VehicleVecUtils.getFrontVec(vehicle.getBodyRotation(partialTick))
        return (atan2(-f.y, sqrt(f.x * f.x + f.z * f.z)) * Mth.RAD_TO_DEG).toFloat()
    }

    /** HUD 用滚转角（度，右滚为正）；机头垂直时退化，回退到欧拉角 */
    fun roll(vehicle: VehicleEntity, partialTick: Float): Float {
        val body = vehicle.getBodyRotation(partialTick)
        val up = VehicleVecUtils.getUpVec(body)
        val right = VehicleVecUtils.getRightVec(body)
        if (abs(up.y) + abs(right.y) < 1.0E-3) return vehicle.getRoll(partialTick)
        return (atan2(-right.y, up.y) * Mth.RAD_TO_DEG).toFloat()
    }

    /** HUD 用航向角（度，MC yaw 语义）；机头垂直时退化，回退到欧拉角 */
    fun yaw(vehicle: VehicleEntity, partialTick: Float): Float {
        val f = VehicleVecUtils.getFrontVec(vehicle.getBodyRotation(partialTick))
        val h = sqrt(f.x * f.x + f.z * f.z)
        if (h < 1.0E-4) return vehicle.yRot
        return (atan2(-f.x, f.z) * Mth.RAD_TO_DEG).toFloat()
    }
}