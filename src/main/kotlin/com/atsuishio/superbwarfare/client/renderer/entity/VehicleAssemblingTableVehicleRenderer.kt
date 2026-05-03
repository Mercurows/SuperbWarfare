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
    ): RenderType? {
        return RenderType.entityTranslucent(getTextureLocation(animatable))
    }

    override fun defaultRender(
        poseStack: PoseStack,
        animatable: VehicleAssemblingTableVehicleEntity,
        bufferSource: MultiBufferSource,
        renderType: RenderType?,
        buffer: VertexConsumer?,
        yaw: Float,
        partialTick: Float,
        packedLight: Int
    ) {
        poseStack.pushPose()
        poseStack.translate(-0.5, 0.0, 0.5)

        val root = Vec3(0.5, 0.5, -0.5)
        poseStack.rotateAround(Axis.YP.rotationDegrees(-yaw), root.x.toFloat(), root.y.toFloat(), root.z.toFloat())
        poseStack.rotateAround(
            Axis.XP.rotationDegrees(Mth.lerp(partialTick, animatable.xRotO, animatable.xRot)),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
        poseStack.rotateAround(
            Axis.ZP.rotationDegrees(Mth.lerp(partialTick, animatable.prevRoll, animatable.roll)),
            root.x.toFloat(),
            root.y.toFloat(),
            root.z.toFloat()
        )
        super.defaultRender(poseStack, animatable, bufferSource, renderType, buffer, yaw, partialTick, packedLight)
        poseStack.popPose()
    }
}
