package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.renderer.SmartTextureBrightener
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.event.ClientEventHandler
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.animatable.GeoAnimatable
import software.bernie.geckolib.model.GeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer
import kotlin.math.max


abstract class VehicleRenderer<T>(renderManager: EntityRendererProvider.Context, model: GeoModel<T>) :
    GeoEntityRenderer<T>(renderManager, model) where T : VehicleEntity, T : GeoAnimatable {

    override fun getRenderType(
        vehicle: T,
        texture: ResourceLocation,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType? = RenderType.entityTranslucent(
        if (ClientEventHandler.activeThermalImaging && ClientEventHandler.thermalImagingMode == 0) SmartTextureBrightener.getSmartBrightenedTexture(
            getTextureLocation(vehicle),
            3f
        ) else getTextureLocation(vehicle)
    )

    override fun render(
        entity: T,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int
    ) {
        poseStack.pushPose()

        val healthRatio: Float = entity.health / entity.getMaxHealth()

        val adjustedLight = this.adjustLightBasedOnHealth(packedLight, healthRatio)

        vehicleAxis(entity, poseStack, entityYaw, partialTick)
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, adjustedLight)
        renderCustomPart(entity, entityYaw, partialTick, poseStack, bufferSource, adjustedLight)
        poseStack.popPose()
    }

    private fun adjustLightBasedOnHealth(packedLight: Int, healthRatio: Float): Int {
        if (healthRatio > 0.5f || (ClientEventHandler.activeThermalImaging && ClientEventHandler.thermalImagingMode == 0)) {
            return packedLight
        }

        // 提取原始的天空光和方块光
        var skyLight = LightTexture.sky(packedLight)
        var blockLight = LightTexture.block(packedLight)

        // 根据生命值比例降低光照
        val dimFactor = (2.0 * healthRatio + 0.2).coerceAtMost(1.0) // 确保不会完全变黑
        skyLight = max((skyLight * dimFactor).toInt().toDouble(), 0.0).toInt()
        blockLight = max((blockLight * dimFactor).toInt().toDouble(), 0.0).toInt()


        // 重新打包光照值
        return LightTexture.pack(blockLight, skyLight)
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
}
