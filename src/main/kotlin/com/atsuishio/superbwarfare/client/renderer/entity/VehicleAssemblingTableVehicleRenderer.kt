package com.atsuishio.superbwarfare.client.renderer.entity

import com.atsuishio.superbwarfare.client.model.entity.VehicleAssemblingTableVehicleModel
import com.atsuishio.superbwarfare.entity.vehicle.VehicleAssemblingTableVehicleEntity
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import software.bernie.geckolib.cache.`object`.BakedGeoModel
import software.bernie.geckolib.renderer.GeoEntityRenderer

class VehicleAssemblingTableVehicleRenderer(renderManager: EntityRendererProvider.Context) :
    GeoEntityRenderer<VehicleAssemblingTableVehicleEntity>(renderManager, VehicleAssemblingTableVehicleModel()) {
    init {
        this.shadowRadius = 0.5f
    }

    override fun getRenderType(
        animatable: VehicleAssemblingTableVehicleEntity,
        texture: ResourceLocation?,
        bufferSource: MultiBufferSource?,
        partialTick: Float
    ): RenderType {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }

    override fun preRender(
        poseStack: PoseStack,
        entity: VehicleAssemblingTableVehicleEntity,
        model: BakedGeoModel,
        bufferSource: MultiBufferSource?,
        buffer: VertexConsumer?,
        isReRender: Boolean,
        partialTick: Float,
        packedLight: Int,
        packedOverlay: Int,
        r: Float,
        g: Float,
        b: Float,
        a: Float
    ) {
        val scale = 1f
        this.scaleHeight = scale
        this.scaleWidth = scale
        super.preRender(
            poseStack,
            entity,
            model,
            bufferSource,
            buffer,
            isReRender,
            partialTick,
            packedLight,
            packedOverlay,
            r,
            g,
            b,
            a
        )
    }

    override fun render(
        entityIn: VehicleAssemblingTableVehicleEntity,
        entityYaw: Float,
        partialTicks: Float,
        poseStack: PoseStack,
        bufferIn: MultiBufferSource,
        packedLightIn: Int
    ) {
        poseStack.pushPose()
        poseStack.translate(-0.5, 0.0, 0.5)

        val root = Vec3(0.5, 0.5, -0.5)
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
        super.render(entityIn, entityYaw, partialTicks, poseStack, bufferIn, packedLightIn)
        poseStack.popPose()
    }
}
