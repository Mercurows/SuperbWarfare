package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.model.entity.VehicleModelBoneGroups
import com.atsuishio.superbwarfare.client.model.entity.VehicleModelInstance
import com.atsuishio.superbwarfare.client.renderer.ModRenderTypes
import com.atsuishio.superbwarfare.client.renderer.TextureBrightnessHandler
import com.atsuishio.superbwarfare.compat.valkyrienskies.ValkyrienSkiesCompat
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.gun.GunProp
import com.atsuishio.superbwarfare.data.vehicle.subdata.SeatInfo
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.data.vehicle_skin.VehicleSkin
import com.atsuishio.superbwarfare.entity.projectile.FastThrowableProjectile
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.VehicleModelEntry
import com.atsuishio.superbwarfare.entity.vehicle.base.RCEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleMotionUtils
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.model.VehicleModelReloadListener
import com.atsuishio.superbwarfare.resource.vehicle.VehicleResource
import com.atsuishio.superbwarfare.script.VehicleScriptManager
import com.atsuishio.superbwarfare.tools.RenderDistanceHelper
import com.atsuishio.superbwarfare.tools.SpritePixelHelper
import com.atsuishio.superbwarfare.tools.localPlayer
import com.atsuishio.superbwarfare.tools.mc
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BoneState
import com.google.common.cache.CacheBuilder
import com.maydaymemory.mae.basic.ArrayPoseBuilder
import com.maydaymemory.mae.basic.ZYXBoneTransformFactory
import com.maydaymemory.mae.blend.EulerAdditiveBlender
import com.maydaymemory.mae.blend.SimpleEulerAdditiveBlender
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.joml.Quaternionf

open class GeoVehicleRenderer<T>(manager: EntityRendererProvider.Context) :
    EntityRenderer<T>(manager) where T : VehicleEntity, T : BasicGeoVehicleEntity {

    var pitch = 0f
    private val CAMERA_TO_MODEL_180 = Quaternionf().rotationY(Math.PI.toFloat())

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
    var hideFlare = false

    private var seatsCache: MutableList<SeatInfo>? = null
    private val mountRenderEntities = CacheBuilder.newBuilder().maximumSize(32).weakValues().build<ResourceLocation, Entity>()

    override fun getTextureLocation(entity: T): ResourceLocation {
        val (_, namespace, id) = entity.type.descriptionId.split(".")
        return ResourceLocation(namespace, "textures/bedrock/vehicle/$id.png")
    }

    open fun getEmissiveTextureLocation(poseStack: PoseStack, entity: T): ResourceLocation? {
        return getCurrentModelEntry(poseStack, entity)?.emissiveTexture
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
        val entry = getCurrentModelEntry(poseStack, entity) ?: return
        val instance = entry.instance
        var texture = entry.texture

        // Apply vehicle skin (only for full-detail models, not LOD)
        val isLOD = entry.isLOD()
        if (!isLOD) {
            val skinInfo = VehicleSkin.getSkin(entity)
            if (skinInfo != null) {
                val skinTexture = ResourceLocation.tryParse(skinInfo.texture)
                if (skinTexture != null) {
                    texture = skinTexture
                }
            }
        }

        texture = if (entity.isWreck) {
            if ((entity.vehicleType == VehicleType.AIRPLANE || entity.vehicleType == VehicleType.HELICOPTER || entity.vehicleType == VehicleType.AIRSHIP || entity.vehicleType == VehicleType.RC)) {
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

        val basePose = poseStack.last()
        poseStack.pushPose()

        this.rotateVehicleAxis(entity, poseStack, yaw, partialTick)
        poseStack.scale(renderScale(), renderScale(), renderScale())

        this.poseModel(entity, instance, partialTick, yaw, poseStack, animate = true, isLOD = isLOD, tickAnimation = true)

        val waterMask = instance.getBone("waterMask")
        val waterFlag = waterMask != null
        if (waterFlag) {
            waterMask.visible = false
        }

        val boneGroups = entry.instance.boneGroups
        val dogTagBones = boneGroups.dogTagBones
        val dogTagFlag = dogTagBones.isNotEmpty()
        if (dogTagFlag) {
            dogTagBones.forEach { it.visible = false }
        }

        val flareBones = boneGroups.flareBones
        val flareFlag = flareBones.isNotEmpty()
        if (flareFlag) {
            for (flare in flareBones) {
                flare.visible = false
                flare.rotation.rotateZ((0.15 * (Math.random() - 0.5)).toFloat())
            }
        }

        instance.renderToBuffer(
            poseStack,
            buffer,
            RenderType.entityTranslucent(texture),
            BedrockModelRenderTypes.polyMeshCutout(texture),
            packedLight,
            OverlayTexture.NO_OVERLAY
        )

        this.renderEmissive(entity, instance, yaw, partialTick, poseStack, buffer, packedLight)

        if (waterFlag) {
            waterMask.visible = true
            val waterMaskIndex = instance.getIndex("waterMask")
            instance.renderSingleBonePass(
                poseStack,
                waterMaskIndex,
                buffer.getBuffer(RenderType.waterMask()),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1f, 1f, 1f, 1f,
                true,
                false
            )
        }

        if (!isLOD && !hideFlare && !entity.sympatheticDetonated && flareFlag && !(ClientEventHandler.zoomVehicle && (hideForTurretControllerWhileZooming || hideForPassengerWeaponStationControllerWhileZooming))) {
            val flareModel = flareModelInstance

            if (flareModel != null) {
                for (flare in flareBones) {
                    poseStack.pushPose()
                    poseStack.mulPoseMatrix(flare.getGlobalTransform(instance))
                    val flareDef = flare.definition()
                    poseStack.translate(
                        flareDef.pivotX(), flareDef.pivotY(), flareDef.pivotZ()
                    )
                    poseStack.translate(0f, 0f, (0.01 * (Math.random() - 0.5)).toFloat())
                    poseStack.scale(
                        1 + (0.02 * (Math.random() - 0.5)).toFloat(),
                        1 + (0.02 * (Math.random() - 0.5)).toFloat(),
                        1 + (0.02 * (Math.random() - 0.5)).toFloat()
                    )
                    flareModel.renderToBuffer(
                        poseStack,
                        buffer,
                        RenderType.entityTranslucentEmissive(MUZZLE_FLARE),
                        ModRenderTypes.POLY_MESH_TRANSLUCENT_EMISSIVE.apply(MUZZLE_FLARE),
                        packedLight,
                        OverlayTexture.NO_OVERLAY,
                        1f,
                        1f,
                        1f,
                        1f
                    )

                    poseStack.popPose()
                }
            }
        }

        val laserBones = boneGroups.laserBones
        val laserFlag = laserBones.isNotEmpty()
        if (laserFlag) {
            customLaserLength(laserBones, entity, partialTick)
        }

        if (!entity.sympatheticDetonated && laserFlag) {
            for (laser in laserBones) {
                poseStack.pushPose()
                poseStack.mulPoseMatrix(laser.getGlobalTransform(instance))
                val laserDef = laser.definition()
                poseStack.translate(
                    laserDef.pivotX(), laserDef.pivotY(), laserDef.pivotZ()
                )

                val lastPose = poseStack.last()
                val pose = lastPose.pose()
                val normal = lastPose.normal()

                val consumerOut = buffer.getBuffer(RenderType.eyes(LASER_TEX))
                val consumerIn = buffer.getBuffer(RenderType.eyes(LASER_TEX_IN))

                val c = entity.laserColor

                val color = Quaternionf(
                    ((c shr 16) and 0xFF).toFloat(),
                    ((c shr 8) and 0xFF).toFloat(),
                    (c and 0xFF).toFloat(),
                    255f
                )
                val colorW = Quaternionf(255f, 255f, 255f, 255f)

                val scale = entity.laserBaseScale.toFloat()

                renderLaser(consumerOut, pose, normal, scale, color)
                renderLaser(consumerIn, pose, normal, scale, colorW)

                poseStack.mulPose(Axis.ZP.rotationDegrees(60f))

                renderLaser(consumerOut, pose, normal, scale, color)
                renderLaser(consumerIn, pose, normal, scale, colorW)

                poseStack.mulPose(Axis.ZP.rotationDegrees(60f))

                renderLaser(consumerOut, pose, normal, scale, color)
                renderLaser(consumerIn, pose, normal, scale, colorW)

                poseStack.mulPose(Axis.ZP.rotationDegrees(60f))

                renderLaser(consumerOut, pose, normal, scale, color)
                renderLaser(consumerIn, pose, normal, scale, colorW)

                poseStack.mulPose(Axis.ZP.rotationDegrees(60f))

                renderLaser(consumerOut, pose, normal, scale, color)
                renderLaser(consumerIn, pose, normal, scale, colorW)

                poseStack.mulPose(Axis.ZP.rotationDegrees(60f))

                renderLaser(consumerOut, pose, normal, scale, color)
                renderLaser(consumerIn, pose, normal, scale, colorW)

                poseStack.popPose()
            }
        }

        // 自定义图章
        if (dogTagFlag && entity.health > 0) {
            val list = entity.dogTagIcon
            val flag = list.all { row -> row.all { it == (-1).toShort() } }
            if (DisplayConfig.DOG_TAG_ICON_VISIBLE.get() && !flag) {
                val dogTagTexture = SpritePixelHelper.getDogTagIcon(list, entity.uuid.toString())

                for (bone in dogTagBones) {
                    val size = VehicleModelBoneGroups.parseDogTagSize(bone.name()) ?: continue

                    poseStack.pushPose()
                    poseStack.mulPoseMatrix(bone.getGlobalTransform(instance))
                    val def = bone.definition()
                    poseStack.translate(def.pivotX() + bone.xScale / 2, def.pivotY() + bone.yScale / 4, def.pivotZ())

                    val pose = poseStack.last()
                    val lastMatrix = pose.pose()
                    val lastMatrix3f = pose.normal()
                    val vertexConsumer =
                        buffer.getBuffer(RenderType.entityCutoutNoCull(dogTagTexture))

                    val (xSize, ySize) = size

                    vertex(vertexConsumer, lastMatrix, lastMatrix3f, packedLight, -0.5f * xSize, -0.5f * ySize, 0, 1)
                    vertex(vertexConsumer, lastMatrix, lastMatrix3f, packedLight, 0.5f * xSize, -0.5f * ySize, 1, 1)
                    vertex(vertexConsumer, lastMatrix, lastMatrix3f, packedLight, 0.5f * xSize, 0.5f * ySize, 1, 0)
                    vertex(vertexConsumer, lastMatrix, lastMatrix3f, packedLight, -0.5f * xSize, 0.5f * ySize, 0, 0)
                    poseStack.popPose()
                }

                buffer.getBuffer(RenderType.entityTranslucent(getTextureLocation(entity)))
            }
        }

        this.renderCustomPart(entity, instance, poseStack, yaw, partialTick, buffer, packedLight)

        if (!isLOD && shouldRenderMounts(entity)) {
            renderMounts(entity, instance, partialTick, basePose, poseStack, buffer, packedLight)
        }
        poseStack.popPose()
    }

    open fun shouldRenderMounts(entity: T): Boolean {
        if (entity.entityData.get(VehicleEntity.MOUNT_STATE).isEmpty) return false

        val resource = VehicleResource.compute(entity)
        if (!resource.renderMounts) return false

        val inRemoteFirstPerson = entity is RCEntity && localPlayer?.vehicle === entity &&
                (mc.options.cameraType.isFirstPerson || ClientEventHandler.zoomVehicle)
        return resource.renderMountsInFirstPerson || !inRemoteFirstPerson
    }

    open fun renderMounts(
        entity: T,
        instance: VehicleModelInstance,
        partialTick: Float,
        basePose: PoseStack.Pose,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
    ) {
        val mounts = entity.mounts
        for (slot in mounts.slots()) {
            val installed = mounts.getInstalled(slot.id) ?: continue
            val definition = mounts.getDefinition(installed.definitionId)
            val display = definition?.display
            val bones = instance.getMountBones(slot.bone, mounts.capacity(slot, definition, installed.stack) > 1)
            val offset = display?.offset ?: Vec3.ZERO
            val rotation = display?.rotation ?: Vec3.ZERO
            val scale = display?.scale ?: 0.4f
            val proxy = display?.entity?.let { getMountRenderEntity(entity, it) }
            val renderer = proxy?.let { entityRenderDispatcher.getRenderer(it) }

            repeat(minOf(installed.stack.count, maxOf(1, bones.size))) { index ->
                poseStack.pushPose()
                try {
                    val bone = bones.getOrNull(index)
                    if (bone != null) {
                        val transform = bone.getGlobalTransform(instance)
                        poseStack.mulPoseMatrix(transform)
                        poseStack.last().normal().mul(transform.normal(Matrix3f()))
                        val pivot = bone.definition()
                        poseStack.translate(pivot.pivotX(), pivot.pivotY(), pivot.pivotZ())
                        poseStack.translate(offset.x, offset.y, offset.z)
                    } else {
                        poseStack.last().pose().set(basePose.pose())
                        poseStack.last().normal().set(basePose.normal())
                        val position = mounts.worldPosition(slot, offset, partialTick).subtract(entity.getPosition(partialTick))
                        poseStack.translate(position.x, position.y, position.z)
                        poseStack.mulPose(Quaternionf(entity.getRotationFromString(slot.transform, partialTick)))
                    }
                    poseStack.mulPose(Axis.XP.rotationDegrees(rotation.x.toFloat()))
                    poseStack.mulPose(Axis.YP.rotationDegrees(rotation.y.toFloat()))
                    poseStack.mulPose(Axis.ZP.rotationDegrees(rotation.z.toFloat()))
                    poseStack.scale(scale, scale, scale)

                    if (proxy != null && renderer != null) {
                        renderer.render(proxy, 0f, partialTick, poseStack, buffer, packedLight)
                    } else {
                        mc.itemRenderer.renderStatic(installed.stack, ItemDisplayContext.FIXED, packedLight,
                            OverlayTexture.NO_OVERLAY, poseStack, buffer, entity.level(), entity.id)
                    }
                } finally {
                    poseStack.popPose()
                }
            }
        }
    }

    open fun getMountRenderEntity(vehicle: T, id: ResourceLocation): Entity? {
        val level = vehicle.level()
        mountRenderEntities.getIfPresent(id)?.takeIf { it.level() === level }?.let { return it }

        val proxy = EntityType.byString(id.toString()).orElse(null)?.create(level) ?: return null
        // Like the vehicle's weapon dummies, show the model before its launch effects begin.
        proxy.tickCount = 2
        if (proxy is FastThrowableProjectile) proxy.syncedTick = proxy.tickCount
        mountRenderEntities.put(id, proxy)
        return proxy
    }

    open fun renderEmissive(
        entity: T,
        instance: VehicleModelInstance,
        yaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        val emissiveTexture = this.getEmissiveTextureLocation(poseStack, entity)
        if (emissiveTexture != null) {
            instance.renderToBuffer(
                poseStack,
                buffer,
                RenderType.eyes(emissiveTexture),
                BedrockModelRenderTypes.polyMeshCutout(emissiveTexture),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }
    }

    open fun customLaserLength(laserBones: List<BoneState>, entity: T, partialTicks: Float) {
        for (laser in laserBones) {
            laser.visible = false

            laser.zScale = 10 * entity.laserLength
            val scale = Mth.lerp(
                partialTicks,
                entity.laserScaleO,
                entity.laserScale
            ).coerceAtMost(1.2f)

            laser.xScale = scale
            laser.yScale = scale
        }
    }

    fun renderLaser(
        consumer: VertexConsumer,
        pose: Matrix4f,
        normal: Matrix3f,
        pX: Float,
        color: Quaternionf
    ) {
        vertex(consumer, pose, normal, -pX, 0f, 0, 1, color)
        vertex(consumer, pose, normal, pX, 0f, 1, 1, color)
        vertex(consumer, pose, normal, pX, 0.1f, 1, 0, color)
        vertex(consumer, pose, normal, -pX, 0.1f, 0, 0, color)
    }

    open fun tickVariables(entity: T, entityYaw: Float, partialTicks: Float) {
        pitch = entity.getPitch(partialTicks)
        yaw = entity.getYaw(partialTicks)
        roll = entity.getRoll(partialTicks)

        leftWheelRot = Mth.lerp(partialTicks, entity.leftWheelRotO, entity.leftWheelRot)
        rightWheelRot = Mth.lerp(partialTicks, entity.rightWheelRotO, entity.rightWheelRot)

        leftTrack = Mth.lerp(partialTicks, entity.leftTrackO, entity.leftTrack)
        rightTrack = Mth.lerp(partialTicks, entity.rightTrackO, entity.rightTrack)

        turretYRot = Mth.lerp(partialTicks, entity.turretYRotO, entity.turretYRot)
        turretXRot = Mth.lerp(partialTicks, entity.turretXRotO, entity.turretXRot)

        turretYaw = entity.getTurretYaw(partialTicks)

        recoilShake = Mth.lerp(partialTicks, entity.recoilShakeO.toFloat(), entity.recoilShake.toFloat())

        hideFlare = false
        hideForTurretControllerWhileZooming =
            ClientEventHandler.zoomVehicle && entity.getNthEntity(entity.turretControllerIndex) === localPlayer
        hideForPassengerWeaponStationControllerWhileZooming =
            ClientEventHandler.zoomVehicle && entity.getNthEntity(entity.passengerWeaponStationControllerIndex) === localPlayer
    }

    open fun renderCustomPart(
        entity: T,
        instance: VehicleModelInstance,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {

        if (entity.health >= 0) {
            val seats = this.seatsCache ?: entity.computed().seats().also { this.seatsCache = it }
            for ((index, seat) in seats.withIndex()) {
                for (k in seat.weapons().indices) {
                    val data = entity.getGunData(index, k) ?: continue
                    val dummyInfo = data.get(GunProp.PROJECTILE_DUMMY_INFO) ?: continue
                    val ammo = data.ammo.get()
                    if (ammo <= 0) continue

                    val projectileInfo = data.get(GunProp.PROJECTILE)
                    val projectileType = projectileInfo.itemId

                    EntityType.byString(projectileType).ifPresent { entityType ->
                        val projectile = entityType.create(entity.level()) ?: return@ifPresent
                        projectile.tickCount = 1
                        if (projectile is FastThrowableProjectile) {
                            projectile.syncedTick = 1
                        }

                        val size = data.get(GunProp.SHOOT_POS).positions.size
                        if (size <= 0) return@ifPresent

                        for (j in 0..<size) {
                            if (j >= ammo) continue

                            val dummyName = "dummy_${index}_${k}_${j + 1}"
                            val bone = instance.getBone(dummyName) ?: continue

                            poseStack.pushPose()
                            poseStack.mulPoseMatrix(bone.getGlobalTransform(instance))
                            val boneDef = bone.definition()
                            poseStack.translate(boneDef.pivotX(), boneDef.pivotY(), boneDef.pivotZ())

                            val scale = dummyInfo.scale
                            poseStack.scale(scale.x.toFloat(), scale.y.toFloat(), scale.z.toFloat())
                            poseStack.mulPose(Axis.YP.rotationDegrees(180f))

                            val rotate = dummyInfo.rotate
                            val quaternion = Quaternionf()
                                .rotateZ(rotate.z.toFloat() * Mth.DEG_TO_RAD)
                                .rotateX(rotate.x.toFloat() * Mth.DEG_TO_RAD)
                                .rotateY(rotate.y.toFloat() * Mth.DEG_TO_RAD)
                            poseStack.mulPose(quaternion)

                            val offset = dummyInfo.offset

                            val flag = dummyInfo.hideDummyWhileZooming && ClientEventHandler.zoomVehicle

                            if (!flag) {
                                entityRenderDispatcher.render(
                                    projectile,
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
    }

    open fun transformCustomModelPart(
        entity: T,
        instance: VehicleModelInstance,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        val boneGroups = instance.boneGroups

        // 车轮
        boneGroups.leftWheels.forEach {
            it.rotation.rotationX(1.5f * leftWheelRot)
        }
        boneGroups.rightWheels.forEach {
            it.rotation.rotationX(1.5f * rightWheelRot)
        }
        boneGroups.leftWheelsTurn.forEach {
            it.rotation.rotationY(Mth.lerp(partialTicks, entity.rudderRotO, entity.rudderRot))
            it.rotation.rotateX(1.5f * leftWheelRot)
        }
        boneGroups.rightWheelsTurn.forEach {
            it.rotation.rotationY(Mth.lerp(partialTicks, entity.rudderRotO, entity.rudderRot))
            it.rotation.rotateX(1.5f * rightWheelRot)
        }

        boneGroups.leftTrackMove.forEachIndexed { index, bone ->
            val t = wrap(leftTrack + getTrackDistance() * index, entity)
            val bd = bone.definition()
            // Fold pivot into y/z — translateAndRotateAndScale discards pivot for unrotated bones
            bone.y = bd.pivotY() * 16f + getBoneMoveY(t)
            bone.z = bd.pivotZ() * 16f + getBoneMoveZ(t)
        }

        boneGroups.rightTrackMove.forEachIndexed { index, bone ->
            val t = wrap(rightTrack + getTrackDistance() * index, entity)
            val bd = bone.definition()
            bone.y = bd.pivotY() * 16f + getBoneMoveY(t)
            bone.z = bd.pivotZ() * 16f + getBoneMoveZ(t)
        }

        boneGroups.leftTrackRot.forEachIndexed { index, bone ->
            val t = wrap(leftTrack + getTrackDistance() * index, entity)
            bone.rotation.rotationX(-getBoneRotX(t) * Mth.DEG_TO_RAD)
        }

        boneGroups.rightTrackRot.forEachIndexed { index, bone ->
            val t = wrap(rightTrack + getTrackDistance() * index, entity)
            bone.rotation.rotationX(-getBoneRotX(t) * Mth.DEG_TO_RAD)
        }

        // 瞄准时隐藏车体
        val hideWhenZooming = hideForTurretControllerWhileZooming() ||
                VehicleResource.compute(entity).hideForTurretControllerWhileZooming
        val root = instance.getBone("root")
        if (root != null && hideWhenZooming) {
            root.visible = !hideForTurretControllerWhileZooming
        }

        // 瞄准时隐藏乘客武器站
        val passengerWeaponStation = instance.getBone("passengerWeaponStation")
        if (passengerWeaponStation != null && hideWhenZooming) {
            passengerWeaponStation.visible = !hideForPassengerWeaponStationControllerWhileZooming
        }

        // 射击时带来的车体摇晃视觉效果
        val base = instance.getBone("base")
        if (base != null) {
            val a = entity.yawWhileShoot
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

            base.rotation.rotationX(r * recoilShake * Mth.DEG_TO_RAD)
            base.rotation.rotateZ(r2 * recoilShake * Mth.DEG_TO_RAD)
        }

        val shipYaw = entity.vehicle?.let {
            if (!ValkyrienSkiesCompat.hasMod()) null
            else ValkyrienSkiesCompat.getShipYaw(it)
        } ?: 0f

        // Turret
        val turret = instance.getBone("turret")
        if (turret != null) {
            turret.rotation.rotationY((turretYRot + shipYaw) * Mth.DEG_TO_RAD)
            turret.visible = !(entity.isWreck && entity.hasTurret() && entity.sympatheticDetonated)
        }

        // Barrel
        val barrel = instance.getBone("barrel")
        if (barrel != null) {
            val rot = Mth.clamp(-turretXRot, entity.turretMinPitch, entity.turretMaxPitch) * Mth.DEG_TO_RAD
            barrel.rotation.rotationX(rot)
        }

        // 乘客武器站
        val passengerWeaponStationYaw = instance.getBone("passengerWeaponStationYaw")
        passengerWeaponStationYaw?.rotation?.rotationY(
            Mth.lerp(
                partialTicks,
                entity.gunYRotO,
                entity.gunYRot
            ) * Mth.DEG_TO_RAD - turretYRot * Mth.DEG_TO_RAD
        )

        val passengerWeaponStationPitch = instance.getBone("passengerWeaponStationPitch")
        passengerWeaponStationPitch?.rotation?.rotationX(
            Mth.clamp(
                -Mth.lerp(
                    partialTicks,
                    entity.gunXRotO,
                    entity.gunXRot
                ) * Mth.DEG_TO_RAD,
                entity.passengerWeaponMinPitch * Mth.DEG_TO_RAD,
                entity.passengerWeaponMaxPitch * Mth.DEG_TO_RAD
            )
        )

        // 武器绑定骨骼
        val seats = this.seatsCache ?: entity.computed().seats().also { this.seatsCache = it }

        for ((index, seat) in seats.withIndex()) {
            for (k in seat.weapons().indices) {
                val data = entity.getGunData(index, k) ?: continue
                val defaultVec = entity.getDefaultBarrelDirection(index, partialTicks) ?: continue
                val targetVec = entity.getShootVec(index, partialTicks) ?: continue
                if (entity.getNthEntity(index) == null) continue
                val boundBones = data.get(GunProp.BOUND_BONES)
                val boundBonesYaw = data.get(GunProp.BOUND_BONES_YAW)
                val boundBonesPitch = data.get(GunProp.BOUND_BONES_PITCH)

                if (boundBones != null) {
                    for (name in boundBones) {
                        val bone = instance.getBone(name)
                        if (bone != null) {
                            // TODO 期待后人智慧，万一哪天正确实现了获取骨骼朝向
                            val diffY = Mth.wrapDegrees(
                                -VehicleVecUtils.getYRotFromVector(targetVec) + VehicleVecUtils.getYRotFromVector(
                                    defaultVec
                                )
                            ).toFloat()
                            val diffX = Mth.wrapDegrees(
                                -VehicleVecUtils.getXRotFromVector(targetVec) + VehicleVecUtils.getXRotFromVector(
                                    defaultVec
                                )
                            ).toFloat()

                            bone.rotation.rotateY(-diffY * Mth.DEG_TO_RAD)
                            bone.rotation.rotateX(-diffX * Mth.DEG_TO_RAD)
                        }
                    }
                }

                if (boundBonesYaw != null) {
                    for (name in boundBonesYaw) {
                        val bone = instance.getBone(name)
                        if (bone != null) {
                            val diffY = Mth.wrapDegrees(
                                -VehicleVecUtils.getYRotFromVector(targetVec) + VehicleVecUtils.getYRotFromVector(
                                    defaultVec
                                )
                            ).toFloat()

                            bone.rotation.rotateY(-diffY * Mth.DEG_TO_RAD)
                        }
                    }
                }

                if (boundBonesPitch != null) {
                    for (name in boundBonesPitch) {
                        val bone = instance.getBone(name)
                        if (bone != null) {
                            val diffX = Mth.wrapDegrees(
                                -VehicleVecUtils.getXRotFromVector(targetVec) + VehicleVecUtils.getXRotFromVector(
                                    defaultVec
                                )
                            ).toFloat()

                            bone.rotation.rotationX(-diffX * Mth.DEG_TO_RAD)
                        }
                    }
                }
            }
        }

        this.transformCustomModelPartByScript(entity, instance, poseStack, entityYaw, partialTicks)
    }

    open fun transformCustomModelPartByScript(
        entity: T,
        model: VehicleModelInstance,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        val func = VehicleResource.getDefault(entity).getScript() ?: return
        VehicleScriptManager.invokeTransform(func, entity, model, poseStack, entityYaw, partialTicks, this)
    }

    /**
     * 统一的模型姿态应用：render() 与骨骼相机共用同一份代码，保证渲染出来的骨骼变换与相机取到的完全一致。
     *
     * @param animate       是否应用动画（LOD / 解体时是 false）
     * @param tickAnimation 是否推进动画；相机传 false，直接使用上一帧 tick 的结果，避免与渲染器重复 tick
     */
    protected fun poseModel(
        entity: T,
        instance: VehicleModelInstance,
        partialTicks: Float,
        entityYaw: Float,
        poseStack: PoseStack,
        animate: Boolean,
        isLOD: Boolean,
        tickAnimation: Boolean
    ) {
        val animation = entity.getAnimationInstance()
        if (animate && animation != null && !isLOD && !entity.sympatheticDetonated) {
            if (tickAnimation) {
                animation.context.partialTick = partialTicks
                animation.tick()
            }
            instance.resetPose()
            instance.applyPose(BLENDER.blend(instance.bindPose, animation.getPose()))
        } else {
            instance.resetPose()
        }

        this.tickVariables(entity, entityYaw, partialTicks)
        this.transformCustomModelPart(entity, instance, poseStack, entityYaw, partialTicks)
    }

    /**
     * 相机专用：把模型摆到与 render() 完全一致的姿态（动画取上一帧 tick 的结果）
     *
     * 会同时刷新 [tickVariables] 的渲染器状态，因此必须在实体渲染之前调用（相机 setup 阶段），
     * 之后渲染器会以同样的值重新计算，不会互相污染。
     */
    @Suppress("UNCHECKED_CAST")
    fun poseModelForCamera(entity: VehicleEntity, instance: VehicleModelInstance, partialTicks: Float, entityYaw: Float) {
        poseModel(entity as T, instance, partialTicks, entityYaw, PoseStack(), animate = true, isLOD = false, tickAnimation = false)
    }

    /**
     * 相机专用：与 render() 中 rotateVehicleAxis + scale 完全一致的「模型空间 → 世界」变换（不含实体自身位置）
     */
    @Suppress("UNCHECKED_CAST")
    fun buildCameraModelTransform(entity: VehicleEntity, partialTicks: Float, entityYaw: Float): Matrix4f {
        val poseStack = PoseStack()
        rotateVehicleAxis(entity as T, poseStack, entityYaw, partialTicks)
        val scale = renderScale()
        poseStack.scale(scale, scale, scale)
        return Matrix4f(poseStack.last().pose())
    }

    open fun rotateVehicleAxis(entity: T, poseStack: PoseStack, entityYaw: Float, partialTicks: Float) {
        val root = Vec3(0.0, entity.rotateOffsetHeight, 0.0)
        if (entity.usesQuaternionBody()) {
            // 四元数载具（直升机/固定翼）：模型旋转 = 数据四元数 * Ry(180°)
            // （模型朝 -Z、数据朝 +Z）。这里不经过欧拉分解，因此不会在 ±90° 附近丢失姿态。
            val q = Quaternionf(entity.getBodyRotation(partialTicks))
                .mul(CAMERA_TO_MODEL_180)
            // fakePitch/fakeRoll 在模型局部轴上叠加（与旧欧拉顺序一致）
            val fakePitch = Mth.lerp(partialTicks, entity.fakePitchO, entity.fakePitch)
            val fakeRoll = Mth.lerp(partialTicks, entity.fakeRollO, entity.fakeRoll)
            // 与旧版模型欧拉顺序一致：Ry(180-yaw)Rx(-(p+fp))Rz(-(r+fr))
            // = q_model * Rz(r) * Rx(-fp) * Rz(-(r+fr))
            val rollDeg = entity.getRoll(partialTicks)
            q.rotateZ(rollDeg * Mth.DEG_TO_RAD)
                .rotateX(-fakePitch * Mth.DEG_TO_RAD)
                .rotateZ(-(rollDeg + fakeRoll) * Mth.DEG_TO_RAD)
            poseStack.translate(root.x, root.y, root.z)
            poseStack.mulPose(q)
            poseStack.translate(-root.x, -root.y, -root.z)
            return
        }
        poseStack.rotateAround(
            Axis.YP.rotationDegrees(-entityYaw + 180),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
        poseStack.rotateAround(
            Axis.XP.rotationDegrees(
                -Mth.lerp(
                    partialTicks,
                    entity.xRotO + entity.fakePitchO,
                    entity.xRot + entity.fakePitch
                )
            ),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(
                -Mth.lerp(
                    partialTicks,
                    entity.prevRoll + entity.fakeRollO,
                    entity.roll + entity.fakeRoll
                )
            ),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
    }

    open fun hideForTurretControllerWhileZooming() = false

    open fun getCurrentModelEntry(poseStack: PoseStack, entity: T): VehicleModelEntry? {
        val entries = entity.getModelEntries()
        return selectModelEntry(entity, entries, poseStack)
    }

    override fun shouldRender(entity: T, pCamera: Frustum, pCamX: Double, pCamY: Double, pCamZ: Double): Boolean {
        if (!entity.shouldRender(pCamX, pCamY, pCamZ)) {
            return false
        } else if (entity.noCulling) {
            return true
        } else {
            var aabb = VehicleMotionUtils.calculateCombinedAABBOptimized(entity).inflate(3.0)

            if (aabb.hasNaN() || aabb.size == 0.0) {
                aabb = AABB(
                    entity.x - 8.0,
                    entity.y - 6.0,
                    entity.z - 8.0,
                    entity.x + 8.0,
                    entity.y + 6.0,
                    entity.z + 8.0
                )
            }

            return pCamera.isVisible(aabb)
        }
    }

    open fun getBoneRotX(t: Float) = t

    open fun getBoneMoveY(t: Float) = t

    open fun getBoneMoveZ(t: Float) = t

    open fun getTrackDistance() = 2f

    open fun wrap(value: Float, range: Int) = ((value % range) + range) % range

    open fun wrap(value: Float, entity: T) = wrap(value, getDefaultWrapRange(entity))

    fun getDefaultWrapRange(entity: T) = entity.getTrackAnimationLength()

    companion object {
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
        val MUZZLE_FLARE = Mod.loc("textures/particle/flare.png")
        val MUZZLE_FLARE_MODEL = Mod.loc("models/bedrock/vehicle/muzzle_flare.geo.json")
        val LASER_TEX_IN = Mod.loc("textures/bedrock/vehicle/laser_in.png")
        val LASER_TEX = Mod.loc("textures/bedrock/vehicle/laser.png")

        val flareModelInstance by lazy {
            VehicleModelReloadListener.getModel(MUZZLE_FLARE_MODEL)?.createInstance()
        }

        @JvmStatic
        fun selectModelEntry(entity: VehicleEntity, entries: List<VehicleModelEntry>, poseStack: PoseStack): VehicleModelEntry? {
            if (entries.isEmpty()) return null
            entries.forEachIndexed { index, entry ->
                if (index == 0) return@forEachIndexed  // skip main model (distance = 0)
                if (RenderDistanceHelper.shouldRenderLOD(entity, poseStack, entry.lodDistance.toDouble())) {
                    return entry
                }
            }
            return entries.firstOrNull()
        }

        private fun vertex(
            pConsumer: VertexConsumer,
            pPose: Matrix4f,
            pNormal: Matrix3f,
            pLightmapUV: Int,
            pX: Float,
            pY: Float,
            pU: Int,
            pV: Int,
        ) {
            pConsumer.vertex(pPose, pX - 0.5f, pY - 0.25f, 0f).color(255, 255, 255, 255).uv(pU.toFloat(), pV.toFloat())
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(pLightmapUV).normal(pNormal, 0f, 1f, 0f).endVertex()
        }

        private fun vertex(
            pConsumer: VertexConsumer,
            pPose: Matrix4f,
            pNormal: Matrix3f,
            pX: Float,
            pZ: Float,
            pU: Int,
            pV: Int,
            color: Quaternionf
        ) {
            pConsumer.vertex(pPose, pX, 0f, -pZ)
                .color(color.x.toInt(), color.y.toInt(), color.z.toInt(), color.w.toInt())
                .uv(pU.toFloat(), pV.toFloat())
                .overlayCoords(OverlayTexture.NO_OVERLAY).uv2(LightTexture.FULL_BRIGHT).normal(pNormal, 0f, 1f, 0f)
                .endVertex()
        }
    }
}
