package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.client.renderer.SmartTextureBrightener
import com.atsuishio.superbwarfare.client.renderer.TextureBrightnessHandler
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.model.VehicleLODModelReloadListener
import com.atsuishio.superbwarfare.resource.model.VehicleModelReloadListener
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.atsuishio.superbwarfare.tools.localPlayer
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Quaterniond
import org.joml.Quaternionf

open class SbmVehicleRenderer<T>(manager: EntityRendererProvider.Context) :
    EntityRenderer<T>(manager) where T : VehicleEntity, T : BasicGeoVehicleEntity {

    protected var pitch = 0f
    protected var yaw = 0f
    protected var roll = 0f
    protected var leftWheelRot = 0f
    protected var rightWheelRot = 0f
    protected var leftTrack = 0f
    protected var rightTrack = 0f

    protected var turretYRot = 0f

    protected var turretXRot = 0f
    protected var turretYaw = 0f
    protected var recoilShake = 0f

    protected var hideForTurretControllerWhileZooming = false
    protected var hideForPassengerWeaponStationControllerWhileZooming = false

    override fun getTextureLocation(entity: T): ResourceLocation {
        val (_, namespace, id) = entity.type.descriptionId.split(".")
        val res = ResourceLocation.fromNamespaceAndPath(namespace, "textures/bedrock/vehicle/$id.png")

        if (ClientEventHandler.activeThermalImaging) {
            return SmartTextureBrightener.getSmartBrightenedTexture(res, 3f)
        } else if (entity.isWreck) {
            return if ((entity.vehicleType == VehicleType.AIRPLANE || entity.vehicleType == VehicleType.HELICOPTER)) {
                if (entity.sympatheticDetonated) {
                    TextureBrightnessHandler.getBrightenedTexture(res, 0.3f)
                } else {
                    res
                }
            } else {
                TextureBrightnessHandler.getBrightenedTexture(res, 0.3f)
            }
        }

        return res
    }

    open fun getEmissiveTextureLocation(entity: T): ResourceLocation? {
        return null
    }

    fun getLODTextureLocation(entity: T, level: Int): ResourceLocation {
        val (_, namespace, id) = entity.type.descriptionId.split(".")
        return ResourceLocation.fromNamespaceAndPath(namespace, "textures/bedrock/vehicle_lod/$id.lod$level.png")
    }

    fun getModelLocation(entity: T): ResourceLocation {
        val (_,  namespace, id) = entity.type.descriptionId.split(".")
        return ResourceLocation.fromNamespaceAndPath(namespace, id)
    }

    fun getLODModelLocation(entity: T, level: Int): ResourceLocation {
        val (_, namespace, id) = entity.type.descriptionId.split(".")
        return ResourceLocation.fromNamespaceAndPath(namespace, "$id.lod$level")
    }

    override fun shouldShowName(pEntity: T): Boolean {
        return false
    }

    override fun render(
        entity: T,
        yaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        var model = VehicleModelReloadListener.getModel(getModelLocation(entity)) ?: return
        var texture = getTextureLocation(entity)
        val emissiveTexture = getEmissiveTextureLocation(entity)

        val lodLevel = getLODLevel(poseStack, entity)
        if (lodLevel > 0) {
            // TODO 等修改好LOD level之后直接获取
            for (i in lodLevel downTo 1) {
                val lod = VehicleLODModelReloadListener.getModel(getLODModelLocation(entity, i)) ?: continue
                model = lod
                texture = getLODTextureLocation(entity, i)
                break
            }
        }

        poseStack.pushPose()

        this.rotateVehicleAxis(entity, poseStack, yaw, partialTick)

        if (entity.getAnimationInstance() != null) {
            val ani = entity.getAnimationInstance()!!
            ani.context.partialTick = partialTick
            ani.tick()
            model.applyPose(BLENDER.blend(model.bindPose, ani.getPose()))
        } else {
            model.applyPose(model.bindPose)
        }

        this.tickVariables(entity, yaw, partialTick)
        this.transformCustomModelPart(entity, model, poseStack, yaw, partialTick)

        val waterMask = model.getBone("waterMask")
        val waterFlag = waterMask != null
        if (waterFlag) {
            waterMask.visible = false
        }

        model.renderToBuffer(
            poseStack,
            buffer,
            RenderType.entityTranslucent(texture),
            BedrockModelRenderTypes.polyMeshCutout(texture),
            packedLight,
            OverlayTexture.NO_OVERLAY
        )

        if (emissiveTexture != null) {
            model.renderToBuffer(
                poseStack,
                buffer,
                RenderType.eyes(emissiveTexture),
                BedrockModelRenderTypes.polyMeshCutout(emissiveTexture),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }

        if (waterFlag) {
            waterMask.visible = true
            waterMask.render(
                poseStack,
                buffer.getBuffer(RenderType.waterMask()),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }

        this.renderCustomPart(entity, model, poseStack, yaw, partialTick, buffer, packedLight)

        poseStack.popPose()
    }

    open fun tickVariables(vehicle: T, entityYaw: Float, partialTicks: Float) {
        pitch = vehicle.getPitch(partialTicks)
        yaw = vehicle.getYaw(partialTicks)
        roll = vehicle.getRoll(partialTicks)

        leftWheelRot = Mth.lerp(partialTicks, vehicle.leftWheelRotO, vehicle.leftWheelRot)
        rightWheelRot = Mth.lerp(partialTicks, vehicle.rightWheelRotO, vehicle.rightWheelRot)

        leftTrack = Mth.lerp(partialTicks, vehicle.leftTrackO, vehicle.leftTrack)
        rightTrack = Mth.lerp(partialTicks, vehicle.rightTrackO, vehicle.rightTrack)

        turretYRot = Mth.lerp(partialTicks, vehicle.turretYRotO, vehicle.turretYRot)
        turretXRot = Mth.lerp(partialTicks, vehicle.turretXRotO, vehicle.turretXRot)

        turretYaw = vehicle.getTurretYaw(partialTicks)

        recoilShake = Mth.lerp(partialTicks, vehicle.recoilShakeO.toFloat(), vehicle.recoilShake.toFloat())

        hideForTurretControllerWhileZooming =
            ClientEventHandler.zoomVehicle && vehicle.getNthEntity(vehicle.turretControllerIndex) === localPlayer
        hideForPassengerWeaponStationControllerWhileZooming =
            ClientEventHandler.zoomVehicle && vehicle.getNthEntity(vehicle.passengerWeaponStationControllerIndex) === localPlayer
    }

    open fun renderCustomPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
    }

    open fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        // Wheels
        model.leftWheels.forEach {
            it.rotation.rotationX(1.5f * leftWheelRot)
        }
        model.rightWheels.forEach {
            it.rotation.rotationX(1.5f * rightWheelRot)
        }
        model.leftWheelsTurn.forEach {
            val yawRot = Axis.YP.rotation(Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))
            val pitchRot = Axis.XP.rotation(1.5f * leftWheelRot)
            val quaternion = Quaterniond(yawRot).mul(Quaterniond(pitchRot))
            it.rotation.mul(Quaternionf(quaternion))
        }
        model.rightWheelsTurn.forEach {
            val yawRot = Axis.YP.rotation(Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot))
            val pitchRot = Axis.XP.rotation(1.5f * rightWheelRot)
            val quaternion = Quaterniond(yawRot).mul(Quaterniond(pitchRot))
            it.rotation.mul(Quaternionf(quaternion))
        }

        // 瞄准时隐藏车体
        val root = model.getBone("root")

        if (root != null && hideForTurretControllerWhileZooming()) {
            root.visible = !hideForTurretControllerWhileZooming
        }

        // 瞄准时隐藏乘客武器站
        val passengerWeaponStation = model.getBone("passengerWeaponStation")

        if (passengerWeaponStation != null && hideForTurretControllerWhileZooming()) {
            passengerWeaponStation.visible = !hideForTurretControllerWhileZooming
        }

        //射击时带来的车体摇晃视觉效果

        val base = model.getBone("base")

        if (base != null) {
            val a = vehicle.yawWhileShoot
            val r = (Mth.abs(a) - 90f) / 90f

            val r2 = if (Mth.abs(a) <= 90f) {
                a / 90f
            } else {
                if (a < 0) {
                    -(180f + a) / 90f
                } else {
                    (180f - a) / 90f
                }
            }

            base.x = -r2 * recoilShake * 0.5f
            base.z = r * recoilShake

            val pitch = Axis.XP.rotationDegrees(r * recoilShake)
            val roll = Axis.ZP.rotationDegrees(r2 * recoilShake)
            val quaternion = Quaterniond(pitch).mul(Quaterniond(roll))
            base.rotation.mul(Quaternionf(quaternion))
        }

        // Turret

        val turret = model.getBone("turret")

        if (turret != null) {
            turret.rotation.rotationY(turretYRot * Mth.DEG_TO_RAD)
            turret.visible = !(vehicle.isWreck && vehicle.hasTurret() && vehicle.sympatheticDetonated)
        }

        //Barrel

        val barrel = model.getBone("barrel")

        if (barrel != null) {
            val rot = Mth.clamp(-turretXRot, vehicle.turretMinPitch, vehicle.turretMaxPitch) * Mth.DEG_TO_RAD
            barrel.rotation.rotationX(rot)
        }

        // Laser

        val laser = model.getBone("laser")

        if (laser != null) {
            laser.zScale = 10 * vehicle.laserLength
            val scale = Mth.lerp(
                partialTicks,
                vehicle.laserScaleO,
                vehicle.laserScale
            ).coerceAtMost(1.2f)

            laser.xScale = scale
            laser.yScale = scale
        }

    }

    open fun rotateVehicleAxis(entityIn: T, poseStack: PoseStack, entityYaw: Float, partialTicks: Float) {
        val root = Vec3(0.0, entityIn.rotateOffsetHeight, 0.0)
        poseStack.rotateAround(
            Axis.YP.rotationDegrees(-entityYaw + 180),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
        poseStack.rotateAround(
            Axis.XP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.xRotO, entityIn.xRot)),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(-Mth.lerp(partialTicks, entityIn.prevRoll, entityIn.roll)),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
    }

    open fun hideForTurretControllerWhileZooming() = false

    fun getLODLevel(poseStack: PoseStack, vehicle: T): Int {
        /** TODO 换成LOD Level
         * 新的LOD level只需要指定n个distance即可
         **/
        val list = listOf(32, 64, 96)
        list.forEachIndexed { index, distance ->
            if (RenderDistanceHelper.shouldRenderLOD(poseStack, distance.toDouble())) {
                return index + 1
            }
        }

        return 0
    }

    override fun shouldRender(vehicle: T, pCamera: Frustum, pCamX: Double, pCamY: Double, pCamZ: Double): Boolean {
        if (!vehicle.shouldRender(pCamX, pCamY, pCamZ)) {
            return false
        } else if (vehicle.noCulling) {
            return true
        } else {
            var aabb = vehicle.boundingBoxForCulling.inflate(5.0)
            if (aabb.hasNaN() || aabb.getSize() == 0.0) {
                aabb = AABB(
                    vehicle.x - 8.0,
                    vehicle.y - 6.0,
                    vehicle.z - 8.0,
                    vehicle.x + 8.0,
                    vehicle.y + 6.0,
                    vehicle.z + 8.0
                )
            }

            return pCamera.isVisible(aabb)
        }
    }

    companion object {
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}