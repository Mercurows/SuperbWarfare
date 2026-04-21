package com.atsuishio.superbwarfare.tools

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.mojang.math.Axis
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.Level
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3i
import org.joml.Vector4d
import java.lang.Math
import kotlin.math.acos
import kotlin.math.sqrt


operator fun Vec3.plus(other: Vec3): Vec3 = add(other)
operator fun Vec3.minus(other: Vec3): Vec3 = subtract(other)
operator fun Vec3.times(factor: Double): Vec3 = scale(factor)
operator fun Vec3.div(factor: Double): Vec3 = scale(1 / factor)
operator fun Vec3.unaryMinus(): Vec3 = reverse()

fun Vec3.toVector3d() = Vector3d(x, y, z)
fun Vec3.toVector3i() = Vector3i(x.toInt(), y.toInt(), z.toInt())
fun Vec3.toBlockPos() = BlockPos(x.toInt(), y.toInt(), z.toInt())

/**
 * 将世界坐标转换为屏幕坐标
 *
 * 感谢 Minecraft-Ping-Wheel 开源
 *
 * https://github.com/LukenSkyne/Minecraft-Ping-Wheel/blob/138295954dab9d2451ad19e16d8d413ef018a2d8/common/src/main/java/nx/pingwheel/common/helper/MathUtils.java#L15>
 */
@OnlyIn(Dist.CLIENT)
fun Vec3.worldToScreen(): Vec3 {
    val window = mc.window
    val camera = mc.gameRenderer.mainCamera
    val worldPosRel = Vector4d(camera.position.reverse().add(this).toVector3f(), 1.0)
    worldPosRel.mul(ClientEventHandler.modelViewMatrix)
    worldPosRel.mul(ClientEventHandler.projectionMatrix)

    val depth = worldPosRel.w

    if (depth != 0.0) {
        worldPosRel.div(depth)
    }

    return Vec3(
        window.guiScaledWidth * (0.5f + worldPosRel.x * 0.5f),
        window.guiScaledHeight * (0.5f - worldPosRel.y * 0.5f),
        depth
    )
}

@OnlyIn(Dist.CLIENT)
@JvmName("canSee")
fun Vec3.canBeSeen(): Boolean {
    val camera = mc.gameRenderer.mainCamera
    val cameraPos = camera.position
    val viewVec = Vec3(camera.lookVector)
    val v1 = cameraPos.vectorTo(this)
    return v1.angleTo(viewVec) < ClientEventHandler.fov
}

fun Vec3.angleTo(other: Vec3): Double {
    val dot = this.dot(other)
    // 检查点积是否为0（垂直情况）
    if (dot == 0.0) return 90.0

    val thisLengthSq = this.lengthSqr()
    val otherLengthSq = other.lengthSqr()

    return if (thisLengthSq > 0.0 && otherLengthSq > 0.0) {
        Math.toDegrees(acos((dot / sqrt(thisLengthSq * otherLengthSq)).coerceIn(-1.0, 1.0)))
    } else {
        0.0
    }
}

fun Vec3.randomPos(radius: Int) =
    this + Vec3(
        Math.random() * radius,
        0.0,
        0.0,
    ).yRot((360 * Math.random()).toFloat() * Mth.DEG_TO_RAD)

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
        return start.angleTo(end)
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
        val turretPitchRot = Axis.XP.rotationDegrees(entity.turretCustomPitch)
        return combineRotations(partialTicks, entity)
            .mul(Quaterniond(turretPitchRot))
            .mul(Quaterniond(turretYawRot))
    }

    @JvmStatic
    fun combineRotationsBarrel(partialTicks: Float, entity: VehicleEntity): Quaterniond {
        val turretPitchRot = Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entity.turretXRotO, entity.turretXRot))
        return combineRotationsTurret(partialTicks, entity)
            .mul(Quaterniond(turretPitchRot))
    }

    @JvmStatic
    fun combineRotationsPassengerWeaponStation(partialTicks: Float, entity: VehicleEntity): Quaterniond {
        val passengerWeaponStationYawRot = Axis.YP.rotationDegrees(
            Mth.lerp(partialTicks, entity.gunYRotO, entity.gunYRot)
                    - Mth.lerp(partialTicks, entity.turretYRotO, entity.turretYRot)
        )
        return combineRotationsTurret(partialTicks, entity)
            .mul(Quaterniond(passengerWeaponStationYawRot))
    }

    @JvmStatic
    fun combineRotationsPassengerWeaponStationBarrel(partialTicks: Float, entity: VehicleEntity): Quaterniond {
        val barrelPitch = Mth.clamp(-Mth.lerp(partialTicks, entity.gunXRotO, entity.gunXRot), entity.passengerWeaponMinPitch, entity.passengerWeaponMaxPitch)
        val passengerWeaponStationPitchRot = Axis.XP.rotationDegrees(-barrelPitch)
        return combineRotationsPassengerWeaponStation(partialTicks, entity)
            .mul(Quaterniond(passengerWeaponStationPitchRot))
    }

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
        val check1 = level.clip(
            ClipContext(
                pos1, pos1.add(pos1.vectorTo(pos2).normalize().scale(128.0)),
                ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, null
            )
        ).type != HitResult.Type.BLOCK

        val check2 = level.clip(
            ClipContext(
                pos2, pos2.add(pos2.vectorTo(pos1).normalize().scale(128.0)),
                ClipContext.Block.VISUAL, ClipContext.Fluid.ANY, null
            )
        ).type != HitResult.Type.BLOCK

        return check1 && check2
    }
}