package com.atsuishio.superbwarfare.client.camera

import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer
import com.atsuishio.superbwarfare.data.vehicle.subdata.CameraBone
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleCameraBoneUtils
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.mc
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Camera
import net.minecraft.client.CameraType
import net.minecraft.util.Mth
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.api.distmarker.OnlyIn
import net.minecraftforge.client.event.ViewportEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import org.joml.Matrix3f
import org.joml.Matrix4d
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector3d

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object VehicleBoneCamera {

    @JvmField
    var activeThisFrame = false

    @JvmField
    var cameraRoll = 0f

    @JvmField
    val activeOrientation = Quaternionf()
    private val CAMERA_TO_MODEL = Quaternionf().rotationY(Math.PI.toFloat())
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
    fun shouldNailCamera(vehicle: VehicleEntity?, player: Player?): Boolean {
        if (vehicle == null || player == null) return false
        val seatIndex = vehicle.getSeatIndex(player)
        if (seatIndex < 0) return false
        if (VehicleCameraBoneUtils.getCameraBone(vehicle, seatIndex) == null) return false
        return ClientEventHandler.zoomVehicle || mc.options.cameraType == CameraType.FIRST_PERSON
    }

    @JvmStatic
    fun resolve(vehicle: VehicleEntity, seatIndex: Int, partialTicks: Float, zoom: Boolean): RigidView? {
        val cameraBone = VehicleCameraBoneUtils.getCameraBone(vehicle, seatIndex) ?: return null
        val instance = VehicleCameraBoneUtils.getMainModelInstance(vehicle) ?: return null
        val renderer = mc.entityRenderDispatcher.getRenderer(vehicle) as? GeoVehicleRenderer<*> ?: return null
        val entityYaw = Mth.lerp(partialTicks, vehicle.yRotO, vehicle.yRot)

        renderer.poseModelForCamera(vehicle, instance, partialTicks, entityYaw)

        val boneNames = VehicleCameraBoneUtils.getBoneNames(cameraBone, zoom)
        val anchor = VehicleCameraBoneUtils.resolveBoneAnchor(instance, boneNames) ?: return null
        val modelToWorld = renderer.buildCameraModelTransform(vehicle, partialTicks, entityYaw)
        val modelToWorldRotation = Quaternionf().setFromUnnormalized(Matrix3f(modelToWorld))
        val boneModelPosition = anchor.transform.transformPosition(Vector3f(anchor.pivot))
        val offset = modelToWorld.transformPosition(boneModelPosition)
        val position = Vec3(
            Mth.lerp(partialTicks.toDouble(), vehicle.xo, vehicle.x) + offset.x,
            Mth.lerp(partialTicks.toDouble(), vehicle.yo, vehicle.y) + offset.y,
            Mth.lerp(partialTicks.toDouble(), vehicle.zo, vehicle.z) + offset.z
        )

        val directionSource = if (zoom) (cameraBone.zoomDirection ?: cameraBone.direction) else cameraBone.direction
        val boneWorldRotation = Quaternionf(modelToWorldRotation)
            .mul(Quaternionf().setFromUnnormalized(Matrix3f(anchor.transform)))

        val orientation = if (directionSource.isNullOrBlank() || directionSource == CameraBone.DIRECTION_BONE) {
            Quaternionf(boneWorldRotation).mul(CAMERA_TO_MODEL)
        } else {
            val directionBone = VehicleCameraBoneUtils.resolveBoneRotation(instance, directionSource)
            var directionWorld: Quaternionf? = null
            val forward = if (directionBone != null) {
                directionWorld = Quaternionf(modelToWorldRotation)
                    .mul(Quaternionf().setFromUnnormalized(Matrix3f(directionBone)))
                val f = directionWorld!!.transform(Vector3f(0f, 0f, -1f))
                Vec3(f.x.toDouble(), f.y.toDouble(), f.z.toDouble())
            } else {
                vehicle.getVectorFromString(directionSource, partialTicks, seatIndex)
            }
            var upHint = boneWorldRotation.transform(Vector3f(0f, 1f, 0f))
            val verticalDot = forward.x * upHint.x + forward.y * upHint.y + forward.z * upHint.z
            if (Math.abs(verticalDot) > 0.9999 && directionWorld != null) {
                upHint = directionWorld!!.transform(Vector3f(0f, 1f, 0f))
            }
            lookRotation(forward, upHint)
        }

        return RigidView(position, orientation)
    }

    @JvmStatic
    fun markActive(orientation: Quaternionf) {
        activeThisFrame = true
        activeOrientation.set(orientation)
    }

    @JvmStatic
    fun clearFrame() {
        activeThisFrame = false
    }

    @JvmStatic
    fun applyViewCorrection(poseStack: PoseStack, camera: Camera) {
        if (!activeThisFrame) return

        val roll = Math.toRadians(cameraRoll.toDouble()).toFloat()
        val pitch = Math.toRadians(camera.getXRot().toDouble()).toFloat()
        val yaw = Math.toRadians((camera.getYRot() + 180.0)).toFloat()

        val vanillaRotation = Quaternionf().rotationZ(roll).rotateX(pitch).rotateY(yaw)
        val desiredViewRotation = Quaternionf(CAMERA_TO_MODEL).mul(Quaternionf(activeOrientation).conjugate())
        val correction = Quaternionf(vanillaRotation).conjugate().mul(desiredViewRotation)

        poseStack.mulPose(correction)

        // 原版在应用相机旋转后就算好了逆视图旋转矩阵（光照用）；这里改写之后必须同步刷新
        RenderSystem.setInverseViewRotationMatrix(Matrix3f(poseStack.last().normal()).invert())
    }

    private fun lookRotation(forward: Vec3, upHint: Vector3f): Quaternionf {
        val f = Vector3f(forward.x.toFloat(), forward.y.toFloat(), forward.z.toFloat())
        if (f.lengthSquared() < 1e-8f) return Quaternionf()
        f.normalize()

        // left = upHint × forward；参考轴与前向平行时换一个参考轴
        TMP_LEFT.set(upHint).cross(f)
        if (TMP_LEFT.lengthSquared() < 1e-8f) {
            TMP_LEFT.set(0f, 1f, 0f).cross(f)
            if (TMP_LEFT.lengthSquared() < 1e-8f) {
                TMP_LEFT.set(1f, 0f, 0f).cross(f)
                if (TMP_LEFT.lengthSquared() < 1e-8f) return Quaternionf()
            }
        }
        TMP_LEFT.normalize()
        TMP_UP.set(f).cross(TMP_LEFT).normalize()

        val basis = Matrix3f().set(TMP_LEFT, TMP_UP, f)
        return Quaternionf().setFromNormalized(basis)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onComputeCameraAngles(event: ViewportEvent.ComputeCameraAngles) {
        cameraRoll = event.roll
    }
}
