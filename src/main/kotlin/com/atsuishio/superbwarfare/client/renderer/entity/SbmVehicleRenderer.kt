package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.BedrockVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.resource.VehicleModelReloadListener
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
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

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
        return ResourceLocation(namespace, "textures/bedrock/vehicle/$id.png")
    }

    fun getModelLocation(entity: T): ResourceLocation {
        val (_,  namespace, id) = entity.type.descriptionId.split(".")
        return ResourceLocation(namespace, "$id.geo")
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
        val model = VehicleModelReloadListener.getModel(getModelLocation(entity)) ?: return

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

        model.renderToBuffer(
            poseStack,
            buffer,
            RenderType.entityCutout(getTextureLocation(entity)),
            BedrockModelRenderTypes.polyMeshCutout(getTextureLocation(entity)),
            packedLight,
            OverlayTexture.NO_OVERLAY
        )

        val texture = entity.getEmissiveTexture()
        if (texture != null) {
            model.renderToBuffer(
                poseStack,
                buffer,
                RenderType.entityCutout(getTextureLocation(entity)),
                BedrockModelRenderTypes.polyMeshCutout(getTextureLocation(entity)),
                packedLight,
                OverlayTexture.NO_OVERLAY
            )
        }

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

    open fun transformCustomModelPart(
        vehicle: T,
        model: BedrockVehicleModel,
        poseStack: PoseStack,
        entityYaw: Float,
        partialTicks: Float
    ) {
        // TODO 实现旋转
        model.leftWheels.forEach {
            it.rotation.mul(Axis.XP.rotationDegrees(1.5f * leftWheelRot))
        }
        model.rightWheels.forEach {
            it.rotation.mul(Axis.XP.rotationDegrees(1.5f * rightWheelRot))
        }
        model.leftWheelsTurn.forEach {
            it.rotation.mul(Axis.XP.rotationDegrees(1.5f * leftWheelRot))
            it.rotation.mul(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot)))
        }
        model.rightWheelsTurn.forEach {
            it.rotation.mul(Axis.XP.rotationDegrees(1.5f * rightWheelRot))
            it.rotation.mul(Axis.YP.rotationDegrees(Mth.lerp(partialTicks, vehicle.rudderRotO, vehicle.rudderRot)))
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

    companion object {
        val BLENDER: EulerAdditiveBlender = SimpleEulerAdditiveBlender(ZYXBoneTransformFactory()) { ArrayPoseBuilder() }
    }
}