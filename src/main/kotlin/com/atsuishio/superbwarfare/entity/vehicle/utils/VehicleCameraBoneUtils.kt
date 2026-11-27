package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer
import com.atsuishio.superbwarfare.compat.valkyrienskies.ValkyrienSkiesCompat
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.vehicle.subdata.CameraBone
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4d
import java.lang.Math

@OnlyIn(Dist.CLIENT)
object VehicleCameraBoneUtils {

    private val warnedMissingBones = HashSet<String>()
    internal class BoneAnchor(val transform: Matrix4f, val pivot: Vector3f)

    @JvmStatic
    fun getCameraBone(seat: SeatInfo?): CameraBone? {
        val data = seat?.cameraBone ?: return null
        return if (data.bone.isBlank()) null else data
    }

    /**
     * 当前选中武器自带的 CameraBone（如投弹睞准镜）。
     * 武器未配置时返回 null。
     */
    @JvmStatic
    fun getWeaponCameraBone(vehicle: VehicleEntity, seatIndex: Int): CameraBone? {
        return vehicle.getGunData(seatIndex)?.get(GunProp.CAMERA_BONE)
    }

    /**
     * 将武器 CameraBone 覆盖到座位 CameraBone 上：
     * - Bone / Direction / ZoomBone / ZoomDirection / ThirdPersonBone 逐项非空则覆盖，否则继承座位；
     * - 因为武器常只配 ZoomBone/ZoomDirection，这样可在保留座位基础跟踪的同时覆盖瞄准鏅。
     */
    @JvmStatic
    fun mergeCameraBone(base: CameraBone?, override: CameraBone?): CameraBone? {
        if (override == null) return base
        if (base == null) return override
        val merged = CameraBone()
        merged.bone = override.bone.ifBlank { base.bone }
        merged.direction = if (override.direction.isNotBlank() && override.direction != CameraBone.DIRECTION_BONE) {
            override.direction
        } else {
            base.direction
        }
        merged.zoomBone = override.zoomBone?.takeIf { it.isNotBlank() } ?: base.zoomBone
        merged.zoomDirection = override.zoomDirection?.takeIf { it.isNotBlank() } ?: base.zoomDirection
        merged.thirdPersonBone = override.thirdPersonBone?.takeIf { it.isNotBlank() } ?: base.thirdPersonBone
        return merged
    }

    @JvmStatic
    fun getCameraBone(vehicle: VehicleEntity, seatIndex: Int): CameraBone? {
        val seat = vehicle.computed().seats().getOrNull(seatIndex)
        val merged = mergeCameraBone(seat?.cameraBone, getWeaponCameraBone(vehicle, seatIndex))
            ?: return null
        return if (merged.bone.isBlank()) null else merged
    }

    /**
     * 当前座位在第一人称（含载具瞄准）视角下是否允许自由视角。
     * 由座位 CameraBone 的 HasFirstPersonFreeCamera 决定，未配置时保持旧行为（允许）。
     */
    @JvmStatic
    fun hasFirstPersonFreeCamera(vehicle: VehicleEntity, seatIndex: Int): Boolean {
        val seat = vehicle.computed().seats().getOrNull(seatIndex) ?: return true
        return seat.cameraBone?.hasFirstPersonFreeCamera ?: true
    }

    /**
     * 当前座位在第三人称视角下是否允许自由视角。
     * 由座位 CameraBone 的 HasThirdPersonFreeCamera 决定，未配置时保持旧行为（允许）。
     */
    @JvmStatic
    fun hasThirdPersonFreeCamera(vehicle: VehicleEntity, seatIndex: Int): Boolean {
        val seat = vehicle.computed().seats().getOrNull(seatIndex) ?: return true
        return seat.cameraBone?.hasThirdPersonFreeCamera ?: true
    }

    /** 当前生效的第三人称骨骼（座位与当前武器 CameraBone 合并后） */
    @JvmStatic
    fun getThirdPersonBone(vehicle: VehicleEntity, seatIndex: Int): String? {
        return getCameraBone(vehicle, seatIndex)?.thirdPersonBone?.takeIf { it.isNotBlank() }
    }

    @JvmStatic
    fun getCameraPosition(vehicle: VehicleEntity, partialTicks: Float, zoom: Boolean, seatIndex: Int): Vec3? {
        val cameraBone = getCameraBone(vehicle, seatIndex) ?: return null
        val instance = getMainModelInstance(vehicle) ?: return null
        val boneNames = getBoneNames(cameraBone, zoom)

        applyVehicleBonePose(vehicle, instance, partialTicks)

        val anchor = resolveBoneAnchor(instance, boneNames) ?: run {
            warnMissingBone(vehicle, boneNames.first())
            return null
        }

        return bonePositionToWorld(vehicle, partialTicks, anchor)
    }

    /** 第三人称位置骨骼名（如 ju_87 驾驶员的 driver_3p_camera） */
    @JvmStatic
    fun getThirdPersonBone(seat: SeatInfo?): String? {
        val data = seat?.cameraBone ?: return null
        return data.thirdPersonBone?.takeIf { it.isNotBlank() }
    }

    /**
     * 计算第三人称骨骼在载具逻辑（数据）坐标系中的偏移（相对 rotateOffsetHeight 枢轴）。
     * 与旧 CameraPos.AircraftCameraPos 同一坐标系，供 getClientVehicleTransform + 旧式环视/滚轮公式复现 UseAircraftCamera。
     */
    @JvmStatic
    fun getThirdPersonBoneOffset(vehicle: VehicleEntity, partialTicks: Float, seatIndex: Int): Vec3? {
        val seat = vehicle.computed().seats().getOrNull(seatIndex) ?: return null
        val boneName = getThirdPersonBone(vehicle, seatIndex) ?: return null
        val instance = getMainModelInstance(vehicle) ?: return null
        applyVehicleBonePose(vehicle, instance, partialTicks)
        val anchor = resolveBoneAnchor(instance, listOf(boneName)) ?: run {
            warnMissingBone(vehicle, boneName)
            return null
        }
        val modelPosition = anchor.transform.transformPosition(Vector3f(anchor.pivot))
        val scale = getRenderScale(vehicle).toDouble()
        // 模型坐标 -> 数据坐标：data = C * model，C = Ry(180°) => (-x, y, -z)；再扣除 rotateOffsetHeight 枢轴
        val dataX = -(modelPosition.x * scale)
        val dataY = modelPosition.y * scale - vehicle.rotateOffsetHeight
        val dataZ = -(modelPosition.z * scale)
        return Vec3(dataX, dataY, dataZ)
    }

    @JvmStatic
    fun getCameraDirection(vehicle: VehicleEntity, partialTicks: Float, zoom: Boolean, seatIndex: Int): Vec3? {
        val cameraBone = getCameraBone(vehicle, seatIndex) ?: return null
        val instance = getMainModelInstance(vehicle) ?: return null
        val boneNames = getBoneNames(cameraBone, zoom)

        applyVehicleBonePose(vehicle, instance, partialTicks)

        val anchor = resolveBoneAnchor(instance, boneNames) ?: run {
            warnMissingBone(vehicle, boneNames.first())
            return null
        }

        val directionSource = if (zoom) (cameraBone.zoomDirection ?: cameraBone.direction) else cameraBone.direction
        if (!directionSource.isNullOrBlank() && directionSource != CameraBone.DIRECTION_BONE) {
            val boneRotation = resolveBoneRotation(instance, directionSource)
            if (boneRotation != null) {
                return boneDirectionToWorld(vehicle, partialTicks, boneRotation)
            }
            return vehicle.getVectorFromString(directionSource, partialTicks, seatIndex)
        }

        return boneDirectionToWorld(vehicle, partialTicks, anchor.transform)
    }

    internal fun getBoneNames(cameraBone: CameraBone, zoom: Boolean): List<String> {
        if (!zoom) return listOf(cameraBone.bone)

        val zoomBone = cameraBone.zoomBone
        return if (zoomBone.isNullOrBlank() || zoomBone == cameraBone.bone) {
            listOf(cameraBone.bone)
        } else {
            listOf(zoomBone, cameraBone.bone)
        }
    }

    @JvmStatic
    fun getMainModelInstance(vehicle: VehicleEntity): VehicleModelInstance? {
        val entries = vehicle.getModelEntries()
        if (entries.isEmpty()) return null
        return (entries.firstOrNull { !it.isLOD() } ?: entries.first()).instance
    }

    private fun applyVehicleBonePose(vehicle: VehicleEntity, instance: VehicleModelInstance, partialTicks: Float) {
        instance.resetPose()

        val turretYRot = Mth.lerp(partialTicks, vehicle.turretYRotO, vehicle.turretYRot)
        val turretXRot = Mth.lerp(partialTicks, vehicle.turretXRotO, vehicle.turretXRot)

        instance.getBone("turret")?.rotation?.rotationY((turretYRot + getShipYaw(vehicle)) * Mth.DEG_TO_RAD)

        instance.getBone("barrel")?.rotation?.rotationX(
            Mth.clamp(-turretXRot, vehicle.turretMinPitch, vehicle.turretMaxPitch) * Mth.DEG_TO_RAD
        )

        instance.getBone("passengerWeaponStationYaw")?.rotation?.rotationY(
            Mth.lerp(partialTicks, vehicle.gunYRotO, vehicle.gunYRot) * Mth.DEG_TO_RAD
                    - turretYRot * Mth.DEG_TO_RAD
        )

        instance.getBone("passengerWeaponStationPitch")?.rotation?.rotationX(
            Mth.clamp(
                -Mth.lerp(partialTicks, vehicle.gunXRotO, vehicle.gunXRot) * Mth.DEG_TO_RAD,
                vehicle.passengerWeaponMinPitch * Mth.DEG_TO_RAD,
                vehicle.passengerWeaponMaxPitch * Mth.DEG_TO_RAD
            )
        )
    }

    private fun getShipYaw(vehicle: VehicleEntity): Float {
        return vehicle.vehicle?.let {
            if (!ValkyrienSkiesCompat.hasMod()) null else ValkyrienSkiesCompat.getShipYaw(it)
        } ?: 0f
    }

    internal fun resolveBoneAnchor(instance: VehicleModelInstance, boneNames: List<String>): BoneAnchor? {
        for (name in boneNames) {
            val bone = instance.getBone(name) ?: continue
            val definition = bone.definition()
            return BoneAnchor(
                instance.getGlobalTransform(bone.index()),
                Vector3f(definition.pivotX(), definition.pivotY(), definition.pivotZ())
            )
        }
        return null
    }

    internal fun resolveBoneRotation(instance: VehicleModelInstance, boneName: String): Matrix4f? {
        val bone = instance.getBone(boneName)
        if (bone != null) {
            return instance.getGlobalTransform(bone.index())
        }
        return instance.getQueryTransform(boneName)
    }

    private fun bonePositionToWorld(
        vehicle: VehicleEntity,
        partialTicks: Float,
        anchor: BoneAnchor
    ): Vec3 {
        val modelPosition = anchor.transform.transformPosition(Vector3f(anchor.pivot))
        return transformModelPosition(vehicle, partialTicks, modelPosition)
    }

    private fun boneDirectionToWorld(
        vehicle: VehicleEntity,
        partialTicks: Float,
        rotationSource: Matrix4f
    ): Vec3? {
        val modelDirection = rotationSource.transformDirection(Vector3f(0f, 0f, -1f))
        if (modelDirection.lengthSquared() < 1.0E-12f) return null

        val worldDirection = getModelToWorldTransform(vehicle, partialTicks).transformDirection(
            Vector3d(modelDirection.x.toDouble(), modelDirection.y.toDouble(), modelDirection.z.toDouble())
        )
        return Vec3(worldDirection.x, worldDirection.y, worldDirection.z).normalize()
    }

    private fun transformModelPosition(vehicle: VehicleEntity, partialTicks: Float, modelPosition: Vector3f): Vec3 {
        val worldPosition = getModelToWorldTransform(vehicle, partialTicks).transform(
            Vector4d(
                modelPosition.x.toDouble(),
                modelPosition.y.toDouble(),
                modelPosition.z.toDouble(),
                1.0
            )
        )
        return Vec3(worldPosition.x, worldPosition.y, worldPosition.z)
    }

    private fun getModelToWorldTransform(vehicle: VehicleEntity, partialTicks: Float): Matrix4d {
        val transform = Matrix4d()

        transform.translate(
            Mth.lerp(partialTicks.toDouble(), vehicle.xo, vehicle.x),
            Mth.lerp(partialTicks.toDouble(), vehicle.yo, vehicle.y),
            Mth.lerp(partialTicks.toDouble(), vehicle.zo, vehicle.z)
        )

        val root = vehicle.rotateOffsetHeight
        transform.translate(0.0, root, 0.0)
        if (vehicle.usesQuaternionBody()) {
            // 四元数载具：模型旋转 = bodyQuat * Ry(π)，再叠加 fakePitch/fakeRoll（模型局部）
            val q = Quaternionf(vehicle.getBodyRotation(partialTicks))
                .mul(Quaternionf().rotationY(Math.PI.toFloat()))
            val fakePitch = Mth.lerp(partialTicks, vehicle.fakePitchO, vehicle.fakePitch)
            val fakeRoll = Mth.lerp(partialTicks, vehicle.fakeRollO, vehicle.fakeRoll)
            val rollDeg = vehicle.getRoll(partialTicks)
            q.rotateZ(rollDeg * Mth.DEG_TO_RAD)
                .rotateX(-fakePitch * Mth.DEG_TO_RAD)
                .rotateZ(-(rollDeg + fakeRoll) * Mth.DEG_TO_RAD)
            transform.rotate(Quaterniond(q.x.toDouble(), q.y.toDouble(), q.z.toDouble(), q.w.toDouble()))
        } else {
            transform.rotateY(
                Math.toRadians((-Mth.lerp(partialTicks, vehicle.yRotO, vehicle.yRot) + 180.0f).toDouble())
            )
            transform.rotateX(
                Math.toRadians(
                    -Mth.lerp(
                        partialTicks,
                        vehicle.xRotO + vehicle.fakePitchO,
                        vehicle.xRot + vehicle.fakePitch
                    ).toDouble()
                )
            )
            transform.rotateZ(
                Math.toRadians(
                    -Mth.lerp(
                        partialTicks,
                        vehicle.prevRoll + vehicle.fakeRollO,
                        vehicle.roll + vehicle.fakeRoll
                    ).toDouble()
                )
            )
        }
        transform.translate(0.0, -root, 0.0)

        val scale = getRenderScale(vehicle).toDouble()
        transform.scale(scale, scale, scale)

        return transform
    }

    internal fun getRenderScale(vehicle: VehicleEntity): Float {
        val renderer = mc.entityRenderDispatcher.getRenderer(vehicle)
        return if (renderer is GeoVehicleRenderer<*>) renderer.renderScale() else 1f
    }

    private fun warnMissingBone(vehicle: VehicleEntity, boneName: String) {
        val key = "position#" + vehicle.type.descriptionId + "#" + boneName
        if (warnedMissingBones.add(key)) {
            Mod.LOGGER.warn(
                "[CameraBone] 载具 {} 的模型里找不到可用的摄像机骨骼 {}" +
                        "（骨骼需要在主模型里存在，且名字包含 camera 以便烘焙时保留为运行时骨骼），已回退到旧的手写视角系统",
                vehicle.type.descriptionId, boneName
            )
        }
    }
}
