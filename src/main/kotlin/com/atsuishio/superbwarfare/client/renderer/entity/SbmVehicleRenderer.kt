package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.entity.vehicle.BasicGeoVehicleEntity
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.github.mcmodderanchor.simplebedrockmodel.v1.common.model.BedrockModel
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
    override fun getTextureLocation(entity: T): ResourceLocation {
        return loc("textures/bedrock/vehicle/${entity.type.descriptionId.split(".")[2]}.png")
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
        val model = BedrockModelLoader.getModel(entity.getModel()) ?: return

        poseStack.pushPose()

        vehicleAxis(entity, poseStack, yaw, partialTick)

        if (entity.getAnimationInstance() != null) {
            val ani = entity.getAnimationInstance()!!
            ani.context.partialTick = partialTick
            ani.tick()
            model.applyPose(BLENDER.blend(model.bindPose, ani.getPose()))
        }

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

        customModelPart(entity, model, poseStack, yaw, partialTick)

        poseStack.popPose()
    }

    open fun customModelPart(entityIn: T, model: BedrockModel, poseStack: PoseStack, entityYaw: Float, partialTicks: Float) {
    }

    open fun vehicleAxis(entityIn: T, poseStack: PoseStack, entityYaw: Float, partialTicks: Float) {
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