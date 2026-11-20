package com.atsuishio.superbwarfare.entity.vehicle.utils

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.camera.VehicleBoneCamera
import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance
import com.atsuishio.superbwarfare.data.vehicle.subdata.CameraBone
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.joml.Matrix4f
import org.joml.Vector3f

@OnlyIn(Dist.CLIENT)
object VehicleCameraBoneUtils {
    private val warnedMissingBones = HashSet<String>()

    internal class BoneAnchor(val transform: Matrix4f, val pivot: Vector3f)

    fun getCameraPosition(vehicle: VehicleEntity, partialTicks: Float, zoom: Boolean, seatIndex: Int): Vec3? =
        VehicleBoneCamera.resolve(vehicle, seatIndex, partialTicks, zoom)?.position

    fun getCameraDirection(vehicle: VehicleEntity, partialTicks: Float, zoom: Boolean, seatIndex: Int): Vec3? {
        val orientation = VehicleBoneCamera.resolve(vehicle, seatIndex, partialTicks, zoom)?.orientation ?: return null
        val forward = orientation.transform(Vector3f(0f, 0f, -1f))
        return Vec3(forward.x.toDouble(), forward.y.toDouble(), forward.z.toDouble())
    }

    internal fun getBoneNames(cameraBone: CameraBone, zoom: Boolean): List<String> {
        val zoomBone = cameraBone.zoomBone
        return if (zoom && !zoomBone.isNullOrBlank() && zoomBone != cameraBone.bone) {
            listOf(zoomBone, cameraBone.bone)
        } else {
            listOf(cameraBone.bone)
        }
    }

    internal fun getMainModelInstance(vehicle: VehicleEntity): VehicleModelInstance? {
        val entries = vehicle.getModelEntries()
        return (entries.firstOrNull { !it.isLOD() } ?: entries.firstOrNull())?.instance
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
        return if (bone != null) instance.getGlobalTransform(bone.index()) else instance.getQueryTransform(boneName)
    }

    internal fun warnMissingBone(vehicle: VehicleEntity, boneName: String) {
        if (warnedMissingBones.add("${vehicle.type.descriptionId}/$boneName")) {
            Mod.LOGGER.warn("[CameraBone] 载具 {} 缺少摄像机骨骼 {}，使用座位的备用视角", vehicle.type.descriptionId, boneName)
        }
    }
}
