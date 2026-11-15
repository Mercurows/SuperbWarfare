package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance
import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer
import com.atsuishio.superbwarfare.compat.valkyrienskies.ValkyrienSkiesCompat
import com.atsuishio.superbwarfare.data.vehicle.subdata.CameraBone
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import org.joml.Matrix4d
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

    @JvmStatic
    fun getCameraBone(vehicle: VehicleEntity, seatIndex: Int): CameraBone? {
        return getCameraBone(vehicle.computed().seats().getOrNull(seatIndex))
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
        transform.translate(0.0, -root, 0.0)

        val scale = getRenderScale(vehicle).toDouble()
        transform.scale(scale, scale, scale)

        return transform
    }

    private fun getRenderScale(vehicle: VehicleEntity): Float {
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
