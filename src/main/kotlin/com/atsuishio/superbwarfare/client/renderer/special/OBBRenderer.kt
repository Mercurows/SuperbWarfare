package com.atsuishio.superbwarfare.client.renderer.special

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import com.atsuishio.superbwarfare.tools.OBB
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.renderer.LevelRenderer
import org.joml.Quaterniond
import org.joml.Quaternionf

/**
 * Codes based on @AnECanSaiTin's [HitboxAPI](https://github.com/AnECanSaiTin/HitboxAPI)
 */
object OBBRenderer {
    fun render(
        entity: VehicleEntity,
        obbList: MutableList<OBB>,
        poseStack: PoseStack,
        buffer: VertexConsumer,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float,
        pPartialTicks: Float
    ) {
        val position = entity.position()
        for (obb in obbList) {
            val center = obb.center
            val halfExtents = obb.extents
            val rotation = obb.rotation
            if (obb.part == OBB.Part.INTERACTIVE) {
                renderOBB(
                    poseStack, buffer,
                    center.x() - position.x(), center.y() - position.y(), center.z() - position.z(),
                    rotation,
                    halfExtents.x(), halfExtents.y(), halfExtents.z(),
                    1f, 0.8f, 0f, 1f
                )
            } else if (obb.part == OBB.Part.COLLISION) {
                renderOBB(
                    poseStack, buffer,
                    center.x() - position.x(), center.y() - position.y(), center.z() - position.z(),
                    rotation,
                    halfExtents.x(), halfExtents.y(), halfExtents.z(),
                    1f, 0f, 0f, 1f
                )
            } else {
                renderOBB(
                    poseStack, buffer,
                    center.x() - position.x(), center.y() - position.y(), center.z() - position.z(),
                    rotation,
                    halfExtents.x(), halfExtents.y(), halfExtents.z(),
                    red, green, blue, alpha
                )
            }
        }
    }

    fun renderOBB(
        poseStack: PoseStack,
        buffer: VertexConsumer,
        centerX: Double,
        centerY: Double,
        centerZ: Double,
        rotation: Quaterniond,
        halfX: Double,
        halfY: Double,
        halfZ: Double,
        red: Float,
        green: Float,
        blue: Float,
        alpha: Float
    ) {
        poseStack.pushPose()
        poseStack.translate(centerX, centerY, centerZ)
        poseStack.mulPose(Quaternionf(rotation.x, rotation.y, rotation.z, rotation.w))
        LevelRenderer.renderLineBox(
            poseStack,
            buffer,
            -halfX,
            -halfY,
            -halfZ,
            halfX,
            halfY,
            halfZ,
            red,
            green,
            blue,
            alpha
        )
        poseStack.popPose()
    }
}
