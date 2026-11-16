package com.atsuishio.superbwarfare.client.camera

import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer
import com.atsuishio.superbwarfare.data.vehicle.subdata.CameraBone
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleCameraBoneUtils
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.neoforged.api.distmarker.Dist
import net.neoforged.api.distmarker.OnlyIn
import org.joml.Matrix3f
import org.joml.Matrix4d
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.lang.Math

@OnlyIn(Dist.CLIENT)
object VehicleBoneCamera {

    @JvmField
    var activeThisFrame = false

    private val TMP_LEFT = Vector3f()
    private val TMP_UP = Vector3f()

    data class RigidView(val position: Vec3, val orientation: Quaternionf)

    /** Convert a logical weapon direction using the same hull pose as the visible model. */
    @JvmStatic
    fun renderedShootDirection(vehicle: VehicleEntity, player: Player, partialTicks: Float): Vec3 {
        val logical = vehicle.getShootVec(player, partialTicks)
        val renderer = mc.entityRenderDispatcher.getRenderer(vehicle) as? GeoVehicleRenderer<*> ?: return logical
        val local = vehicle.getVehicleTransform(partialTicks).invert().transformDirection(
            Vector3d(logical.x, logical.y, logical.z)
        )
        // The model faces -Z; the logical vehicle faces +Z. Include the renderer's
        // inertial pitch/roll here, just as the bone camera and visible barrel do.
        val renderedHull = Matrix4d(renderer.buildCameraModelTransform(
            vehicle, partialTicks, Mth.lerp(partialTicks, vehicle.yRotO, vehicle.yRot)
        )).rotateY(Math.PI)
        val direction = renderedHull.transformDirection(local).normalize()
        return Vec3(direction.x, direction.y, direction.z)
    }

    @JvmStatic
    fun resolve(vehicle: VehicleEntity, seatIndex: Int, partialTicks: Float, zoom: Boolean): RigidView? {
        val cameraBone = vehicle.computed().seats().getOrNull(seatIndex)?.cameraBone ?: return null
        if (cameraBone.bone.isBlank()) return null
        val instance = VehicleCameraBoneUtils.getMainModelInstance(vehicle) ?: return null
        val renderer = mc.entityRenderDispatcher.getRenderer(vehicle) as? GeoVehicleRenderer<*> ?: return null
        val entityYaw = Mth.lerp(partialTicks, vehicle.yRotO, vehicle.yRot)

        val boneNames = VehicleCameraBoneUtils.getBoneNames(cameraBone, zoom)
        val modelToWorld = renderer.poseModelForCamera(vehicle, instance, partialTicks, entityYaw)
        val anchor = VehicleCameraBoneUtils.resolveBoneAnchor(instance, boneNames) ?: run {
            VehicleCameraBoneUtils.warnMissingBone(vehicle, boneNames.first())
            return null
        }
        val boneModelPosition = anchor.transform.transformPosition(Vector3f(anchor.pivot))
        val offset = modelToWorld.transformPosition(boneModelPosition)
        val position = Vec3(
            Mth.lerp(partialTicks.toDouble(), vehicle.xo, vehicle.x) + offset.x,
            Mth.lerp(partialTicks.toDouble(), vehicle.yo, vehicle.y) + offset.y,
            Mth.lerp(partialTicks.toDouble(), vehicle.zo, vehicle.z) + offset.z
        )

        val directionSource = if (zoom) (cameraBone.zoomDirection ?: cameraBone.direction) else cameraBone.direction
        val boneWorldRotation = Quaternionf()
            .setFromUnnormalized(Matrix3f(modelToWorld).mul(Matrix3f(anchor.transform)))
            .normalize()

        // Camera 和 Bedrock 模型的前向都是 -Z。
        val orientation = if (directionSource.isNullOrBlank() || directionSource == CameraBone.DIRECTION_BONE) {
            boneWorldRotation
        } else {
            val directionTransform = VehicleCameraBoneUtils.resolveBoneRotation(instance, directionSource)?.let {
                Matrix3f(modelToWorld).mul(Matrix3f(it))
            }
            val forward = directionTransform?.transform(Vector3f(0f, 0f, -1f)) ?: run {
                val vector = vehicle.getVectorFromString(directionSource, partialTicks, seatIndex)
                Vector3f(vector.x.toFloat(), vector.y.toFloat(), vector.z.toFloat())
            }
            val upHint = boneWorldRotation.transform(Vector3f(0f, 1f, 0f))
            if (directionTransform != null && forward.lengthSquared() > 1e-8f &&
                kotlin.math.abs(Vector3f(forward).normalize().dot(upHint)) > 0.9999f
            ) {
                directionTransform.transform(upHint.set(0f, 1f, 0f)).normalize()
            }
            lookRotation(forward, upHint) ?: boneWorldRotation
        }

        return RigidView(position, orientation)
    }

    @JvmStatic
    fun markActive() {
        activeThisFrame = true
    }

    @JvmStatic
    fun clearFrame() {
        activeThisFrame = false
    }

    private fun lookRotation(forward: Vector3f, upHint: Vector3f): Quaternionf? {
        if (!forward.isFinite || forward.lengthSquared() < 1e-8f) return null
        forward.normalize()

        // left = upHint × forward；参考轴与前向平行时换一个参考轴
        TMP_LEFT.set(upHint).cross(forward)
        if (TMP_LEFT.lengthSquared() < 1e-8f) {
            TMP_LEFT.set(0f, 1f, 0f).cross(forward)
            if (TMP_LEFT.lengthSquared() < 1e-8f) {
                TMP_LEFT.set(1f, 0f, 0f).cross(forward)
                if (TMP_LEFT.lengthSquared() < 1e-8f) return null
            }
        }
        TMP_LEFT.normalize()
        TMP_UP.set(forward).cross(TMP_LEFT).normalize()

        val basis = Matrix3f().set(TMP_LEFT.negate(), TMP_UP, forward.negate())
        return Quaternionf().setFromNormalized(basis)
    }
}
