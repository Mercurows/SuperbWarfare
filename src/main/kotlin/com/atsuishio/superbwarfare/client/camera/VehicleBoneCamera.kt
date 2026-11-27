package com.atsuishio.superbwarfare.client.camera

import com.atsuishio.superbwarfare.client.renderer.entity.GeoVehicleRenderer
import com.atsuishio.superbwarfare.data.vehicle.subdata.CameraBone
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleCameraBoneUtils
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.event.ClientMouseHandler
import com.atsuishio.superbwarfare.tools.getMaxZoom
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
import org.joml.Vector4d
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

    /** 第三人称骨骼相机：位置绑定 3p 骨骼、旋转包含 body roll，保留自由视角 */
    @JvmStatic
    fun shouldNailThirdPersonCamera(vehicle: VehicleEntity?, player: Player?): Boolean {
        if (vehicle == null || player == null) return false
        if (ClientEventHandler.zoomVehicle) return false
        if (mc.options.cameraType == CameraType.FIRST_PERSON) return false
        val seatIndex = vehicle.getSeatIndex(player)
        if (seatIndex < 0) return false
        if (VehicleCameraBoneUtils.getCameraBone(vehicle, seatIndex) == null) return false
        return VehicleCameraBoneUtils.getThirdPersonBone(vehicle, seatIndex) != null
    }

    /**
     * 第三人称骨骼相机：
     * - 位置直接绑定到 ThirdPersonBone 的世界位置（含 body roll）；
     * - 基础朝向 = 载具本体朝向（含 roll）；
     * - 自由视角 = 在本体局部轴上叠加 freeYaw/freePitch（保留旧方向感）；
     * - 滚轮距离沿本体后方/上方平移（与旧 UseAircraftCamera 的 z-/y+ 方向一致，但随 body roll 旋转）。
     */
    @JvmStatic
    fun resolveThirdPerson(vehicle: VehicleEntity, seatIndex: Int, partialTicks: Float): RigidView? {
        val boneName = VehicleCameraBoneUtils.getThirdPersonBone(vehicle, seatIndex) ?: return null
        val instance = VehicleCameraBoneUtils.getMainModelInstance(vehicle) ?: return null
        val renderer = mc.entityRenderDispatcher.getRenderer(vehicle) as? GeoVehicleRenderer<*> ?: return null
        val entityYaw = Mth.lerp(partialTicks, vehicle.yRotO, vehicle.yRot)

        renderer.poseModelForCamera(vehicle, instance, partialTicks, entityYaw)

        val anchor = VehicleCameraBoneUtils.resolveBoneAnchor(instance, listOf(boneName)) ?: return null
        val modelToWorld = renderer.buildCameraModelTransform(vehicle, partialTicks, entityYaw)
        val modelToWorldRotation = Quaternionf().setFromUnnormalized(Matrix3f(modelToWorld))
        val boneModelPosition = anchor.transform.transformPosition(Vector3f(anchor.pivot))
        val offset = modelToWorld.transformPosition(boneModelPosition)
        var position = Vec3(
            Mth.lerp(partialTicks.toDouble(), vehicle.xo, vehicle.x) + offset.x,
            Mth.lerp(partialTicks.toDouble(), vehicle.yo, vehicle.y) + offset.y,
            Mth.lerp(partialTicks.toDouble(), vehicle.zo, vehicle.z) + offset.z
        )

        // 基础朝向：骨骼（当前为 root 子骨骼）的世界旋转。Camera 框架 forward=+Z 下需乘 Ry(180°) 对齐模型 -Z。
        val baseOrientation = Quaternionf(modelToWorldRotation)
            .mul(Quaternionf().setFromUnnormalized(Matrix3f(anchor.transform)))
            .mul(CAMERA_TO_MODEL)

        // 自由视角：与旧 UseAircraftCamera 一致的“世界 Y yaw + 相机局部 pitch”；roll=0 时严格等价 (yaw-freeYaw, pitch+freePitch)
        // 座位 CameraBone 的 HasThirdPersonFreeCamera 关闭时，第三人称相机固定跟随本体朝向
        val allowFree = VehicleCameraBoneUtils.hasThirdPersonFreeCamera(vehicle, seatIndex)
        val qFree = if (allowFree) {
            Quaternionf()
                .rotationY(ClientMouseHandler.freeCameraYaw.toFloat() * Mth.DEG_TO_RAD)
                .mul(baseOrientation)
                .rotateX(ClientMouseHandler.freeCameraPitch.toFloat() * Mth.DEG_TO_RAD)
        } else {
            Quaternionf(baseOrientation)
        }

        // 滚轮拉远/拉近：沿本体后方（-forward）与上方（+up）0.1 倍
        val distance = ClientMouseHandler.custom3pDistanceLerp
        if (distance != 0.0) {
            val forward = Quaternionf(baseOrientation).transform(Vector3f(0f, 0f, 1f))
            val up = Quaternionf(baseOrientation).transform(Vector3f(0f, 1f, 0f))
            position = position.add(
                -forward.x * distance + up.x * 0.1 * distance,
                -forward.y * distance + up.y * 0.1 * distance,
                -forward.z * distance + up.z * 0.1 * distance
            )
        }

        // 自由视角环视：保持相机到载具 root（载具位置 + rotateOffsetHeight）的远离，
        // 用“自由视观旋转相对基础姿态的差异”去旋转该偏移，而不是固定骨骼位置原地转头。
        if (!allowFree || (ClientMouseHandler.freeCameraYaw == 0.0 && ClientMouseHandler.freeCameraPitch == 0.0)) {
            return RigidView(position, baseOrientation)
        }
        val rotationDelta = Quaternionf(qFree)
            .mul(Quaternionf(baseOrientation).conjugate())
            .normalize()
        val root = Vec3(
            Mth.lerp(partialTicks.toDouble(), vehicle.xo, vehicle.x),
            Mth.lerp(partialTicks.toDouble(), vehicle.yo, vehicle.y) + vehicle.rotateOffsetHeight,
            Mth.lerp(partialTicks.toDouble(), vehicle.zo, vehicle.z)
        )
        val rootOffset = Vector3f(
            (position.x - root.x).toFloat(),
            (position.y - root.y).toFloat(),
            (position.z - root.z).toFloat()
        )
        rotationDelta.transform(rootOffset)
        position = Vec3(root.x + rootOffset.x, root.y + rootOffset.y, root.z + rootOffset.z)

        // 保留旧 UseAircraftCamera 的遮挡裁剪：从载具枢轴向目标相机位置做连线检测
        position = getMaxZoom(
            vehicle.getClientVehicleTransform(partialTicks),
            Vector4d(position.x, position.y, position.z, 1.0)
        )

        return RigidView(position, qFree)
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

        // 主驾飞行员第一人称/瞄准也保留自由视角（基于骨骼基础朝向；第三人称由 resolveThirdPerson 处理）。
        // 仅 ThirdPersonBone 存在的驾驶员座位启用，避免影响炮手/旧骨骼座位。
        var finalOrientation = orientation
        if (cameraBone.thirdPersonBone != null &&
            VehicleCameraBoneUtils.hasFirstPersonFreeCamera(vehicle, seatIndex) &&
            (ClientMouseHandler.freeCameraYaw != 0.0 || ClientMouseHandler.freeCameraPitch != 0.0)
        ) {
            finalOrientation = Quaternionf()
                .rotationY(ClientMouseHandler.freeCameraYaw.toFloat() * Mth.DEG_TO_RAD)
                .mul(orientation)
                .rotateX(ClientMouseHandler.freeCameraPitch.toFloat() * Mth.DEG_TO_RAD)
        }

        return RigidView(position, finalOrientation)
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
