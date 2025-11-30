package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.mojang.math.Axis
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.acos

operator fun Vec3.plus(other: Vec3): Vec3 = add(other)
operator fun Vec3.minus(other: Vec3): Vec3 = subtract(other)
operator fun Vec3.times(factor: Double): Vec3 = scale(factor)
operator fun Vec3.div(factor: Double): Vec3 = scale(1 / factor)
operator fun Vec3.unaryMinus(): Vec3 = reverse()

fun Vec3.toVector3d() = Vector3d(x, y, z)
fun Vec3.toVector3i() = Vector3i(x.toInt(), y.toInt(), z.toInt())

fun Vec3.toBlockPos() = BlockPos(x.toInt(), y.toInt(), z.toInt())
fun Vector3f.toVec3() = Vec3(x.toDouble(), y.toDouble(), z.toDouble())
fun Vector3d.toVec3() = Vec3(x, y, z)
fun Vector3i.toVec3() = Vec3(x.toDouble(), y.toDouble(), z.toDouble())

operator fun Vec2.plus(other: Vec2): Vec2 = add(other)
operator fun Vec2.times(factor: Float): Vec2 = scale(factor)
operator fun Vec2.div(factor: Float): Vec2 = scale(1 / factor)
operator fun Vec2.unaryMinus(): Vec2 = negated()

object VectorTool {
    @JvmStatic
    fun calculateAngle(start: Vec3, end: Vec3): Double {
        val startLength = start.length()
        val endLength = end.length()
        return if (startLength > 0 && endLength > 0) {
            Math.toDegrees(acos((start.dot(end) / (startLength * endLength)).coerceIn(-1.0, 1.0)))
        } else {
            0.0
        }
    }

    @JvmStatic
    fun calculateY(x: Float): Float {
        return if (x < -90) {
            -(x + 180.0f) / 90.0f   // x ∈ [-180, -90)
        } else if (x <= 90) {
            x / 90.0f               // x ∈ [-90, 90]
        } else {
            (180.0f - x) / 90.0f    // x ∈ (90, 180]
        }
    }

    // 合并三个旋转（Yaw -> Pitch -> Roll）
    @JvmStatic
    fun combineRotations(partialTicks: Float, entity: VehicleEntity): Quaterniond {
        // 1. 获取三个独立的旋转四元数
        val yawRot = Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entity.yRotO, entity.yRot))
        val pitchRot = Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.xRotO, entity.xRot))
        val rollRot = Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entity.prevRoll, entity.roll))

        // 2. 按照正确顺序合并：先Yaw，再Pitch，最后Roll
        return Quaterniond(yawRot)  // 初始化为Yaw旋转
            .mul(Quaterniond(pitchRot))     // 应用Pitch旋转
            .mul(Quaterniond(rollRot))      // 应用Roll旋转
    }

    // 仅水平旋转
    @JvmStatic
    fun combineRotationsYaw(partialTicks: Float, entity: VehicleEntity) =
        Quaterniond(Axis.YP.rotationDegrees(-Mth.lerp(partialTicks, entity.yRotO, entity.yRot)))


    @JvmStatic
    fun combineRotationsTurret(partialTicks: Float, entity: VehicleEntity): Quaterniond {
        val turretYawRot = Axis.YP.rotationDegrees(Mth.lerp(partialTicks, entity.turretYRotO, entity.turretYRot))
        return combineRotations(partialTicks, entity)
            .mul(Quaterniond(turretYawRot))
    }

    @JvmStatic
    fun combineRotationsBarrel(partialTicks: Float, entity: VehicleEntity): Quaterniond {
        val turretPitchRot = Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.turretXRotO, entity.turretXRot))
        return combineRotationsTurret(partialTicks, entity)
            .mul(Quaterniond(turretPitchRot))
    }

    @JvmStatic
    fun randomPos(originPos: Vec3, radius: Int) =
        originPos + Vec3(
            Math.random() * radius,
            0.0,
            0.0,
        ).yRot((360 * Math.random()).toFloat() * Mth.DEG_TO_RAD)

    @JvmStatic
    fun isInLiquid(level: Level, position: Vec3): Boolean {
        // 将 Vec3 转换为 BlockPos（获取所在方块位置）
        val blockPos = BlockPos.containing(position)

        // 获取该位置的流体状态
        val fluidState = level.getFluidState(blockPos)

        // 检查流体是否有效且位置低于流体表面
        if (fluidState.isEmpty) return false

        // 获取流体在方块中的高度（0 - 1）
        val fluidHeight = fluidState.getHeight(level, blockPos)
        // 计算位置相对于当前方块底部的偏移量
        val yOffset = position.y - blockPos.y
        // 如果位置低于流体表面则返回 true
        return yOffset < fluidHeight
    }

    /**
     * 计算镜面反射向量。
     *
     * @param v1 入射向量（弹射物的方向向量，如运动向量）。
     * @param v0 平面法向量（朝向向量）。
     * @return 反射向量 v2。
     */
    fun calculateReflection(v1: Vec3, v0: Vec3): Vec3 {
        // 归一化法向量（确保单位长度）

        // 计算点积 v1 · n

        val dot = v1.dot(v0)

        // 计算反射向量: v2 = v1 - 2 * (v1 · n) * n
        return v1 - v0 * (2 * dot)
    }

    @JvmStatic
    fun lerpGetEntityBoundingBoxCenter(entity: Entity, partialTick: Float): Vec3 {
        return Vec3(
            Mth.lerp(partialTick.toDouble(), entity.xo, entity.x),
            Mth.lerp(
                partialTick.toDouble(),
                entity.yo + entity.bbHeight / 2,
                entity.y + entity.bbHeight / 2
            ),
            Mth.lerp(partialTick.toDouble(), entity.zo, entity.z)
        )
    }

    @JvmStatic
    fun checkNoClip(pos1: Vec3, pos2: Vec3, level: Level): Boolean {
        return level.clip(
            ClipContext(
                pos1, pos2,
                ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, null
            )
        ).type != HitResult.Type.BLOCK
    }
}