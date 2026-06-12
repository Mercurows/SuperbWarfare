package com.atsuishio.superbwarfare.entity.projectile

import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3

/**
 * 追踪制导接口 — 标记一个投射物实体具有目标追踪/制导能力
 *
 * 提供 [turn] 和 [turnYaw] 的默认实现，子类只需提供目标相关 getter/setter 方法和制导类型
 */
interface ITrackableProjectile {
    /** 追踪目标的世界坐标 */
    fun getTargetPos(): Vec3?
    fun setTargetPos(value: Vec3?)

    /** 追踪目标的 UUID 字符串 */
    fun getTargetUUID(): String
    fun setTargetUUID(value: String)

    /** 制导类型 */
    fun getGuideType(): Int
    fun setGuideType(value: Int)

    /** 是否被诱饵弹干扰 */
    fun isDistracted(): Boolean
    fun setDistracted(value: Boolean)

    /** 是否已丢失制导（如玩家停止瞄准） */
    fun isLost(): Boolean
    fun setLost(value: Boolean)

    /** 是否丢失追踪目标 */
    fun isLostTarget(): Boolean
    fun setLostTarget(value: Boolean)

    /**
     * 全角度转向（Yaw + Pitch），逐步将当前速度方向转向目标方向
     *
     * @param vec3 目标方向向量
     * @param turnSpeed 最大转向速度（度/tick）
     */
    fun turn(vec3: Vec3, turnSpeed: Float) {
        val self = this as? Entity ?: return
        var adjVec3 = vec3
        val v0 = self.deltaMovement.normalize()

        adjVec3 = adjVec3.add(v0.scale(-0.4))

        val d0 = adjVec3.horizontalDistance()
        val targetAngleY = (-Mth.atan2(adjVec3.x, adjVec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        val targetAngleX = (-Mth.atan2(adjVec3.y, d0) * (180f / Math.PI.toFloat()).toDouble()).toFloat()

        val diffY = Mth.wrapDegrees(targetAngleY - self.yRot)
        val diffX = Mth.wrapDegrees(targetAngleX - self.xRot)

        self.deltaMovement = self.deltaMovement.scale(1 - 0.0004 * VehicleVecUtils.calculateAngle(adjVec3, v0))
        self.yRot += (0.95f * diffY).coerceIn(-turnSpeed, turnSpeed)
        self.xRot += (0.95f * diffX).coerceIn(-turnSpeed, turnSpeed)
    }

    /**
     * 仅 Yaw 转向，不改 Pitch
     *
     * @param vec3 目标方向向量
     * @param turnSpeed 最大转向速度（度/tick）
     */
    fun turnYaw(vec3: Vec3, turnSpeed: Float) {
        val self = this as? Entity ?: return
        var adjVec3 = vec3
        val v0 = self.deltaMovement.normalize()

        adjVec3 = adjVec3.add(v0.scale(-0.4))

        val targetAngleY = (-Mth.atan2(adjVec3.x, adjVec3.z) * (180f / Math.PI.toFloat()).toDouble()).toFloat()
        val diffY = Mth.wrapDegrees(targetAngleY - self.yRot)

        self.deltaMovement = self.deltaMovement.scale(1 - 0.0004 * VehicleVecUtils.calculateAngle(adjVec3, v0))
        self.yRot += (0.95f * diffY).coerceIn(-turnSpeed, turnSpeed)
    }
}
