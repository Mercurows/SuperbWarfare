package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.renderer.SmartTextureBrightener
import com.atsuishio.superbwarfare.client.renderer.TextureBrightnessHandler
import com.atsuishio.superbwarfare.client.tooltip.ClientDogTagImageTooltip
import com.atsuishio.superbwarfare.config.client.DisplayConfig
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.atsuishio.superbwarfare.init.ModItems
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
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import org.joml.Matrix3f
import org.joml.Matrix4f
import software.bernie.geckolib.cache.`object`.GeoBone
import software.bernie.geckolib.core.animatable.GeoAnimatable
import software.bernie.geckolib.core.animatable.model.CoreGeoBone
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer
import software.bernie.geckolib.util.RenderUtils
import top.theillusivec4.curios.api.CuriosApi


abstract class VehicleRenderer<T>(renderManager: EntityRendererProvider.Context, model: GeoModel<T>) :
    GeoEntityRenderer<T>(renderManager, model) where T : VehicleEntity, T : GeoAnimatable {

//    // 太掉帧了，暂时不用
//    init {
//        this.addRenderLayer(VehicleDamageLayer(this as GeoRenderer<T>) as GeoRenderLayer<T>?)
//    }

    override fun getRenderType(
        vehicle: T,
        texture: ResourceLocation,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? = RenderType.entityTranslucent(getTextureLocation(vehicle))

    override fun render(entityIn: T, entityYaw: Float, partialTicks: Float, poseStack: PoseStack, bufferIn: MultiBufferSource, packedLightIn: Int) {
        poseStack.pushPose()
        vehicleAxis(entityIn, poseStack, entityYaw, partialTicks)
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn)
        renderCustomPart(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn)
        poseStack.popPose()
    }

    override fun renderRecursively(poseStack: PoseStack, animatable: T, bone: GeoBone?, renderType: RenderType?, bufferSource: MultiBufferSource, bufferIn: VertexConsumer?, isReRender: Boolean, partialTick: Float, packedLight: Int, packedOverlay: Int, red: Float, green: Float, blue: Float, alpha: Float) {
        val name = bone!!.name
        //TODO 能不能优化成用狗牌右键载具，让载具缓存一个图案，这样就能拥有多辆不同图章的载具了
        if (name.endsWith("_dogTag")) {
            bone.isHidden = true
            if (animatable.lastDogTagOwner is LivingEntity) {
                val living = animatable.lastDogTagOwner as LivingEntity
                if (shouldRenderDogTagIcon(living)) {
                    poseStack.pushPose()
                    RenderUtils.translateMatrixToBone(poseStack, bone)
                    RenderUtils.translateToPivotPoint(poseStack, bone)
                    RenderUtils.rotateMatrixAroundBone(poseStack, bone)
                    RenderUtils.scaleMatrixForBone(poseStack, bone)
                    RenderUtils.translateAwayFromPivotPoint(poseStack, bone)
                    poseStack.translate(bone.pivotX / 16, bone.pivotY / 16, bone.pivotZ / 16)
                    poseStack.mulPose(Axis.YP.rotationDegrees(180f))
                    poseStack.mulPose(Axis.XP.rotationDegrees(90f))

                    val `$$6`: PoseStack.Pose = poseStack.last()
                    val `$$7` = `$$6`.pose()
                    val `$$8` = `$$6`.normal()
                    val `$$9`: VertexConsumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(SpritePixelHelper.getDogTagIcon(living)))

                    val scale = bone.cubes[0].size
                    val xSize = scale.x.toFloat() / 16
                    val ySize = scale.y.toFloat() / 16

                    vertex(`$$9`, `$$7`, `$$8`, packedLight, -0.5f * xSize, -0.5f * ySize, 0, 1)
                    vertex(`$$9`, `$$7`, `$$8`, packedLight, 0.5f * xSize, -0.5f * ySize, 1, 1)
                    vertex(`$$9`, `$$7`, `$$8`, packedLight, 0.5f * xSize, 0.5f * ySize, 1, 0)
                    vertex(`$$9`, `$$7`, `$$8`, packedLight, -0.5f * xSize, 0.5f * ySize, 0, 0)
                    poseStack.popPose()
                }
                bufferSource.getBuffer(RenderType.entityTranslucent(getTextureLocation(animatable)))
            }
        }

        super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, bufferIn, isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha)
    }

    fun rotateMatrixAroundBone(poseStack: PoseStack, bone: CoreGeoBone) {
        if (bone.rotZ != 0f) poseStack.mulPose(Axis.ZP.rotation(bone.rotZ))

        if (bone.rotY != 0f) poseStack.mulPose(Axis.YP.rotation(bone.rotY))

        if (bone.rotX != 0f) poseStack.mulPose(Axis.XP.rotation(bone.rotX))
    }

    private fun shouldRenderDogTagIcon(living: LivingEntity?): Boolean {
        val flag = booleanArrayOf(false)
        CuriosApi.getCuriosInventory(living).ifPresent { c ->
            c.findFirstCurio(ModItems.DOG_TAG.get()).ifPresent { s ->
                val stack = s.stack()
                if (ClientDogTagImageTooltip.shouldRenderIcon(stack)) {
                    flag[0] = true
                }
            }
        }
        return flag[0] && DisplayConfig.DOG_TAG_ICON_VISIBLE.get()
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
