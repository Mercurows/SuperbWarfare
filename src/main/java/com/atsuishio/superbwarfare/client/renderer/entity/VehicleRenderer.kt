package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.layer.vehicle.VehicleDamageLayer
import com.atsuishio.superbwarfare.client.renderer.SmartTextureBrightener
import com.atsuishio.superbwarfare.client.renderer.TextureBrightnessHandler
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleType
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.core.animatable.GeoAnimatable
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer
import software.bernie.geckolib.renderer.GeoRenderer
import software.bernie.geckolib.renderer.layer.GeoRenderLayer


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
            if ((animatable.vehicleType == VehicleType.AIRPLANE || animatable.vehicleType == VehicleType.HELICOPTER) && animatable.sympatheticDetonated) {
                return TextureBrightnessHandler.getBrightenedTexture(res, 0.3f)
            } else {
                return res
            }
        }
        return res
    }
}
