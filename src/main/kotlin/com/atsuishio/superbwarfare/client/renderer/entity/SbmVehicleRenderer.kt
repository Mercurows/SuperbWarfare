package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.SmartTextureBrightener
import com.atsuishio.superbwarfare.client.renderer.TextureBrightnessHandler
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.model.VehicleLODModelReloadListener
import com.atsuishio.superbwarfare.resource.model.VehicleModelReloadListener
import com.atsuishio.superbwarfare.resource.vehicle.VehicleModelPojo
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.atsuishio.superbwarfare.tools.SpritePixelHelper
import com.atsuishio.superbwarfare.tools.localPlayer
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaterniond
import org.joml.Quaternionf

open class SbmVehicleRenderer<T>(manager: EntityRendererProvider.Context) :
    EntityRenderer<T>(manager) where T : VehicleEntity, T : BasicGeoVehicleEntity {

    var pitch = 0f
    var yaw = 0f
    var roll = 0f
    var leftWheelRot = 0f
    var rightWheelRot = 0f
    var leftTrack = 0f
    var rightTrack = 0f

    var turretYRot = 0f

    var turretXRot = 0f
    var turretYaw = 0f
    var recoilShake = 0f

    var hideForTurretControllerWhileZooming = false
    var hideForPassengerWeaponStationControllerWhileZooming = false

    private var seatsCache: MutableList<SeatInfo>? = null

    override fun getTextureLocation(entity: T): ResourceLocation {
        val (_, namespace, id) = entity.type.descriptionId.split(".")
        return ResourceLocation(namespace, "textures/bedrock/vehicle/$id.png")
    }

    open fun getEmissiveTextureLocation(poseStack: PoseStack, entity: T): ResourceLocation? {
        return this.getCurrentModel(poseStack, entity)?.emissiveTexture
    }

    open fun renderScale(): Float {
        return 1f
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
        val currentModel = this.getCurrentModel(poseStack, entity) ?: return
        val modelPath = currentModel.model ?: return
        var texture = currentModel.texture ?: return

        val isLOD = currentModel.isLOD()
        val model = if (isLOD) {
            VehicleLODModelReloadListener.getModel(modelPath)
        } else {
            VehicleModelReloadListener.getModel(modelPath)
        } ?: return

        val emissiveTexture = this.getEmissiveTextureLocation(poseStack, entity)
        texture = if (ClientEventHandler.activeThermalImaging) {
            SmartTextureBrightener.getSmartBrightenedTexture(texture, 3f)
        } else if (entity.isWreck) {
            if ((entity.vehicleType == VehicleType.AIRPLANE || entity.vehicleType == VehicleType.HELICOPTER || entity.vehicleType == VehicleType.AIRSHIP)) {
                if (entity.sympatheticDetonated) {
                    TextureBrightnessHandler.getBrightenedTexture(texture, 0.3f)
                } else {
                    texture
                }
            } else {
                TextureBrightnessHandler.getBrightenedTexture(texture, 0.3f)
            }
        } else {
            texture
        }

        poseStack.pushPose()

        this.rotateVehicleAxis(entity, poseStack, yaw, partialTick)
        poseStack.scale(renderScale(), renderScale(), renderScale())

        if (entity.getAnimationInstance() != null && !isLOD && !entity.sympatheticDetonated) {
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

        val dogTagBones = model.dogTagBones
        val dogTagFlag = dogTagBones.isNotEmpty()
        if (dogTagFlag) {
            dogTagBones.forEach { it.visible = false }
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

        val flareBones = model.flareBones
        val flareFlag = flareBones.isNotEmpty()
        if (flareFlag) {
            for (flare in flareBones) {
                flare.visible = false
                flare.rotation.rotateZ((0.5 * (Math.random() - 0.5)).toFloat())
            }
        }

        if (!isLOD && !entity.sympatheticDetonated && flareFlag && !(ClientEventHandler.zoomVehicle && (hideForTurretControllerWhileZooming || hideForPassengerWeaponStationControllerWhileZooming))) {
            val flareModel = VehicleModelReloadListener.getModel(MUZZLE_FLARE_MODEL)

            if (flareModel != null) {
                for (flare in flareBones) {
                    poseStack.pushPose()
                    poseStack.mulPoseMatrix(flare.globalTransform)
                    flareModel.renderToBuffer(
                        poseStack,
                        buffer,
                        ModRenderTypes.MUZZLE_FLASH_TYPE.apply(MUZZLE_FLARE),
                        BedrockModelRenderTypes.polyMeshCutout(MUZZLE_FLARE),
                        packedLight,
                        OverlayTexture.NO_OVERLAY
                    )
                    flareModel.applyPose(flareModel.bindPose)

                    poseStack.popPose()
                }
            }
        }

        // 自定义图章
        if (dogTagFlag && entity.health > 0) {
            val list = entity.dogTagIcon
            val flag = list.all { row -> row.all { it == (-1).toShort() } }
            if (DisplayConfig.DOG_TAG_ICON_VISIBLE.get() && !flag) {
                val dogTagTexture = SpritePixelHelper.getDogTagIcon(list, entity.uuid.toString())

                for (bone in dogTagBones) {
                    poseStack.pushPose()
                    poseStack.mulPoseMatrix(bone.getGlobalTransform())
                    poseStack.mulPose(Axis.YP.rotationDegrees(180f))
                    poseStack.mulPose(Axis.XP.rotationDegrees(90f))

                    val pose = poseStack.last()
                    val lastMatrix = pose.pose()
                    val lastMatrix3f = pose.normal()
                    val vertexConsumer =
                        buffer.getBuffer(RenderType.entityCutoutNoCull(dogTagTexture))

                    val cube = bone.cubes.firstOrNull()
                    val xSize = cube?.width() ?: 1f
                    val ySize = cube?.height() ?: 1f

                    vertex(vertexConsumer, lastMatrix, lastMatrix3f, packedLight, -0.5f * xSize, -0.5f * ySize, 0, 1)
                    vertex(vertexConsumer, lastMatrix, lastMatrix3f, packedLight, 0.5f * xSize, -0.5f * ySize, 1, 1)
                    vertex(vertexConsumer, lastMatrix, lastMatrix3f, packedLight, 0.5f * xSize, 0.5f * ySize, 1, 0)
                    vertex(vertexConsumer, lastMatrix, lastMatrix3f, packedLight, -0.5f * xSize, 0.5f * ySize, 0, 0)
                    poseStack.popPose()
                }

                buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)))
            }
        }

        this.renderCustomPart(entity, model, poseStack, yaw, partialTick, buffer, packedLight)

        poseStack.popPose()
    }

    private fun vertex(
        pConsumer: VertexConsumer,
        pPose: Matrix4f,
        pNormal: Matrix3f,
        pLightmapUV: Int,
        pX: Float,
        pZ: Float,
        pU: Int,
        pV: Int
    ) {
        pConsumer.vertex(pPose, pX, 0f, -pZ).color(255, 255, 255, 255).uv(pU.toFloat(), pV.toFloat())
            .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(pLightmapUV).normal(pNormal, 0f, 1f, 0f).endVertex()
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
        val seats = this.seatsCache ?: vehicle.computed().seats().also { this.seatsCache = it }

        for ((index, seat) in seats.withIndex()) {
            for (k in seat.weapons().indices) {
                val data = vehicle.getGunData(index, k) ?: continue
                val dummyInfo = data.get(GunProp.PROJECTILE_DUMMY_INFO) ?: continue
                val ammo = data.ammo.get()
                if (ammo <= 0) continue

                val projectileInfo = data.get(GunProp.PROJECTILE)
                val projectileType = projectileInfo.itemId

                EntityType.byString(projectileType).ifPresent { entityType ->
                    val entity = entityType.create(vehicle.level()) ?: return@ifPresent
                    entity.tickCount = 1

                    val size = data.get(GunProp.SHOOT_POS).positions.size
                    if (size <= 0) return@ifPresent

                    for (j in 0..<size) {
                        if (j >= ammo) continue

                        val dummyName = "dummy_${index}_${k}_${j + 1}"
                        val bone = model.getBone(dummyName) ?: continue

                        poseStack.pushPose()
                        poseStack.mulPoseMatrix(bone.globalTransform)

                        val scale = dummyInfo.scale

                        poseStack.scale(scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
                        poseStack.mulPose(Axis.YP.rotationDegrees(180f))

                        val rotate = dummyInfo.rotate

                        val yawRot = Axis.YP.rotation(rotate.y.toFloat())
                        val pitchRot = Axis.XP.rotation(rotate.x.toFloat())
                        val rollRot = Axis.ZP.rotation(rotate.z.toFloat())
                        val quaternion = Quaterniond(yawRot).mul(Quaterniond(pitchRot)).mul(Quaterniond(rollRot))
                        poseStack.mulPose(Quaternionf(quaternion))

                        val offset = dummyInfo.offset

                        val flag = dummyInfo.hideDummyWhileZooming && ClientEventHandler.zoomVehicle

                        if (!flag) {
                            entityRenderDispatcher.render(
                                entity,
                                offset.x,
                                offset.y,
                                offset.z,
                                entityYaw,
                                partialTicks,
                                poseStack,
                                buffer,
                                packedLight
                            )
                        }

                        poseStack.popPose()
                    }
                }
            }
        }
    }

    open fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        // 车轮
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

        // 履带
        model.leftTrackMove.forEachIndexed { index, bone ->
            val t = wrap(leftTrack + getTrackDistance() * index, vehicle)
            bone.y += getBoneMoveY(t)
            bone.z += getBoneMoveZ(t)
        }

        model.rightTrackMove.forEachIndexed { index, bone ->
            val t = wrap(rightTrack + getTrackDistance() * index, vehicle)
            bone.y += getBoneMoveY(t)
            bone.z += getBoneMoveZ(t)
        }

        model.leftTrackRot.forEachIndexed { index, bone ->
            val t = wrap(leftTrack + getTrackDistance() * index, vehicle)
            bone.rotation.rotationX(-getBoneRotX(t) * Mth.DEG_TO_RAD)
        }

        model.rightTrackRot.forEachIndexed { index, bone ->
            val t = wrap(rightTrack + getTrackDistance() * index, vehicle)
            bone.rotation.rotationX(-getBoneRotX(t) * Mth.DEG_TO_RAD)
        }

        // 瞄准时隐藏车体
        val root = model.getBone("root")

        if (root != null && hideForTurretControllerWhileZooming()) {
            root.visible = !hideForTurretControllerWhileZooming
        }

        // 瞄准时隐藏乘客武器站
        val passengerWeaponStation = model.getBone("passengerWeaponStation")

        if (passengerWeaponStation != null && hideForTurretControllerWhileZooming()) {
            passengerWeaponStation.visible = !hideForPassengerWeaponStationControllerWhileZooming
        }

        // 射击时带来的车体摇晃视觉效果
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

        // Barrel
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

        // 乘客武器站
        val passengerWeaponStationYaw = model.getBone("passengerWeaponStationYaw")

        passengerWeaponStationYaw?.rotation?.rotationY(
            Mth.lerp(
                partialTicks,
                vehicle.gunYRotO,
                vehicle.gunYRot
            ) * Mth.DEG_TO_RAD - turretYRot * Mth.DEG_TO_RAD
        )

        val passengerWeaponStationPitch = model.getBone("passengerWeaponStationPitch")

        passengerWeaponStationPitch?.rotation?.rotationX(
            Mth.clamp(
                -Mth.lerp(
                    partialTicks,
                    vehicle.gunXRotO,
                    vehicle.gunXRot
                ) * Mth.DEG_TO_RAD,
                vehicle.passengerWeaponMinPitch * Mth.DEG_TO_RAD,
                vehicle.passengerWeaponMaxPitch * Mth.DEG_TO_RAD
            )
        )
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

    open fun getCurrentModel(poseStack: PoseStack, vehicle: T): VehicleModelPojo? {
        val models = VehicleResource.compute(vehicle).getModels()
        if (models.isEmpty()) return null
        models.forEachIndexed { index, model ->
            if (index == 0) return@forEachIndexed
            if (RenderDistanceHelper.shouldRenderLOD(poseStack, model.distance.toDouble())) {
                return model
            }
        }
        return models.first()
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

    open fun getBoneRotX(t: Float) = t

    open fun getBoneMoveY(t: Float) = t

    open fun getBoneMoveZ(t: Float) = t

    open fun getTrackDistance() = 2f

    protected fun wrap(value: Float, range: Int) = ((value % range) + range) % range

    protected fun wrap(value: Float, vehicle: VehicleEntity) = wrap(value, getDefaultWrapRange(vehicle))

    fun getDefaultWrapRange(vehicle: VehicleEntity) = vehicle.getTrackAnimationLength()

    companion object {
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
        val MUZZLE_FLARE = Mod.loc("textures/particle/flare.png")
        val MUZZLE_FLARE_MODEL = Mod.loc("models/bedrock/vehicle/muzzle_flare.geo.json")
    }
}