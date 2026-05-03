package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.renderer.SmartTextureBrightener
import com.atsuishio.superbwarfare.client.renderer.TextureBrightnessHandler
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.tools.SpritePixelHelper
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Quaternionf
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer
import software.bernie.geckolib.util.RenderUtil


abstract class VehicleRenderer<T>(renderManager: EntityRendererProvider.Context, model: GeoModel<T>) :
    GeoEntityRenderer<T>(renderManager, model) where T : VehicleEntity, T : GeoAnimatable {

    override fun getRenderType(
        vehicle: T,
        texture: ResourceLocation,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? = RenderType.entityTranslucent(getTextureLocation(vehicle))


    override fun render(
        entity: T,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int
    ) {
        poseStack.pushPose()
        vehicleAxis(entity, poseStack, entityYaw, partialTick)
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight)
        renderCustomPart(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight)
        poseStack.popPose()
    }

    override fun renderRecursively(
        poseStack: PoseStack,
        animatable: T,
        bone: GeoBone,
        renderType: RenderType?,
        bufferSource: MultiBufferSource,
        bufferIn: VertexConsumer?,
        isReRender: Boolean,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int,
        color: Int
    ) {
        val name = bone.name
        if (name.endsWith("_dogTag")) {
            bone.isHidden = true
            val list = animatable.dogTagIcon
            val flag = list.all { row -> row.all { it == (-1).toShort() } }
            if (DisplayConfig.DOG_TAG_ICON_VISIBLE.get() && !flag) {
                poseStack.pushPose()
                RenderUtil.translateMatrixToBone(poseStack, bone)
                RenderUtil.translateToPivotPoint(poseStack, bone)
                RenderUtil.rotateMatrixAroundBone(poseStack, bone)
                RenderUtil.scaleMatrixForBone(poseStack, bone)
                RenderUtil.translateAwayFromPivotPoint(poseStack, bone)
                poseStack.translate(bone.pivotX / 16, bone.pivotY / 16, bone.pivotZ / 16)
                poseStack.mulPose(Axis.YP.rotationDegrees(180f))
                poseStack.mulPose(Axis.XP.rotationDegrees(90f))

                val pose = poseStack.last()
                val lastMatrix = pose.pose()
                val vertexConsumer =
                    bufferSource.getBuffer(RenderType.entityCutoutNoCull(SpritePixelHelper.getDogTagIcon(list, animatable.uuid.toString())))

                val scale = bone.cubes[0].size
                val xSize = scale.x.toFloat() / 16
                val ySize = scale.y.toFloat() / 16

                vertex(vertexConsumer, lastMatrix, pose, packedLight, -0.5f * xSize, -0.5f * ySize, 0, 1)
                vertex(vertexConsumer, lastMatrix, pose, packedLight, 0.5f * xSize, -0.5f * ySize, 1, 1)
                vertex(vertexConsumer, lastMatrix, pose, packedLight, 0.5f * xSize, 0.5f * ySize, 1, 0)
                vertex(vertexConsumer, lastMatrix, pose, packedLight, -0.5f * xSize, 0.5f * ySize, 0, 0)
                poseStack.popPose()

                bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(animatable)))
            }
        }

        super.renderRecursively(
            poseStack,
            animatable,
            bone,
            renderType,
            bufferSource,
            bufferIn,
            isReRender,
            partialTick,
            packedLight,
            packedOverlay,
            color
        )
    }

    private fun rotateMatrixAroundBone(poseStack: PoseStack, bone: GeoBone) {
        if (bone.rotZ != 0f || bone.rotY != 0f || bone.rotX != 0f) {
            poseStack.mulPose(Quaternionf().rotationZYX(bone.rotZ, bone.rotY, bone.rotX))
        }
    }

    private fun vertex(
        pConsumer: VertexConsumer,
        pPose: Matrix4f,
        pNormal: PoseStack.Pose,
        pLightmapUV: Int,
        pX: Float,
        pZ: Float,
        pU: Int,
        pV: Int
    ) {
        pConsumer.addVertex(pPose, pX, 0f, -pZ)
            .setColor(255, 255, 255, 255)
            .setUv(pU.toFloat(), pV.toFloat())
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(pLightmapUV)
            .setNormal(pNormal, 0f, 1f, 0f)
    }

    open fun vehicleAxis(entityIn: T, poseStack: PoseStack, entityYaw: Float, partialTicks: Float) {
        val root = Vec3(0.0, entityIn.rotateOffsetHeight, 0.0)
        poseStack.rotateAround(
            Axis.YP.rotationDegrees(-entityYaw),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
        poseStack.rotateAround(
            Axis.XP.rotationDegrees(Mth.lerp(partialTicks, entityIn.xRotO, entityIn.xRot)),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(Mth.lerp(partialTicks, entityIn.prevRoll, entityIn.roll)),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
    }

    fun renderCustomPart(
        entityIn: T,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack?,
        bufferIn: MultiBufferSource,
        packedLightIn: Int
    ) {
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

    override fun getTextureLocation(animatable: T): ResourceLocation {
        val res = super.getTextureLocation(animatable)
        if (ClientEventHandler.activeThermalImaging) {
            return SmartTextureBrightener.getSmartBrightenedTexture(res, 3f)
        } else if (animatable.isWreck) {
            return if ((animatable.vehicleType == VehicleType.AIRPLANE || animatable.vehicleType == VehicleType.HELICOPTER)) {
                if (animatable.sympatheticDetonated) {
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
}
