package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientRenderHandler
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.tools.localPlayer
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

class ProjectileEntityRenderer(manager: EntityRendererProvider.Context) : EntityRenderer<ProjectileEntity>(manager) {
    override fun getTextureLocation(pEntity: ProjectileEntity) = loc("textures/entity/empty.png")

    override fun shouldRender(
        pLivingEntity: ProjectileEntity,
        pCamera: Frustum,
        pCamX: Double,
        pCamY: Double,
        pCamZ: Double
    ): Boolean {
        return true
    }

    // 渲染方式参考 ywzj_vehicle
    // 非常的永无，非常的止境（嗯OC）
    override fun render(
        entity: ProjectileEntity,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        val instance = entity.modelInstance ?: return
        val eyePos = localPlayer?.eyePosition ?: return

        poseStack.pushPose()

        ClientRenderHandler.transformVirtualRenderPosition(poseStack, entity, partialTick)

        val width = Math.min(entity.tickCount * 0.15f, 0.6f)
        val position = entity.getPosition(partialTick)
        val distance = position.distanceTo(eyePos)
        val length = 0.7 * entity.deltaMovement.length()

        if (entity.hasHoming() && entity.homingTrail.isNotEmpty()) {
            if (entity.tickCount >= 2 || distance > 4.0) {
                renderHomingTrail(entity, position, poseStack, buffer, packedLight, width)
            }
        }

        poseStack.mulPose(Axis.YP.rotationDegrees(VehicleVecUtils.getYRotFromVector(entity.deltaMovement).toFloat()))
        poseStack.mulPose(Axis.XP.rotationDegrees(-VehicleVecUtils.getXRotFromVector(entity.deltaMovement).toFloat()))
        poseStack.scale(width, width, length.toFloat())

        if (entity.tickCount >= 2 || distance > 4.0) {
            val type = RenderType.energySwirl(TEXTURE, 15.0f, 15.0f)
            instance.renderToBuffer(
                poseStack,
                buffer.getBuffer(type),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                entity.getEntityData().get(ProjectileEntity.COLOR_R),
                entity.getEntityData().get(ProjectileEntity.COLOR_G),
                entity.getEntityData().get(ProjectileEntity.COLOR_B),
                1.0f
            )
        }

        poseStack.popPose()
    }

    /** Volumetric segments stay visible when viewed along the firing direction. */
    private fun renderHomingTrail(
        entity: ProjectileEntity, head: Vec3, poseStack: PoseStack, buffer: MultiBufferSource,
        packedLight: Int, width: Float
    ) {
        val instance = entity.modelInstance ?: return
        val vertices = buffer.getBuffer(RenderType.energySwirl(TEXTURE, 15.0f, 15.0f))
        val maxLength = maxOf(1.0, entity.deltaMovement.length() * 1.5)
        var travelled = 0.0
        var from = head
        val r = entity.getEntityData().get(ProjectileEntity.COLOR_R)
        val g = entity.getEntityData().get(ProjectileEntity.COLOR_G)
        val b = entity.getEntityData().get(ProjectileEntity.COLOR_B)
        for (sample in entity.homingTrail) {
            val delta = sample.subtract(from)
            val length = delta.length()
            if (length < 1.0e-5) continue
            val used = minOf(length, maxLength - travelled)
            if (used <= 0.0) break
            val end = from.add(delta.scale(used / length))
            // Bedrock projectile geometry extends backward along its local Z axis.
            // Orient each segment like a bullet travelling from the older point to the newer one.
            val forward = from.subtract(end)
            val relative = from.subtract(head)
            val fade = (1.0 - (travelled + used * 0.5) / maxLength).toFloat()
            val thickness = width * (0.35f + 0.65f * fade)
            poseStack.pushPose()
            poseStack.translate(relative.x, relative.y, relative.z)
            poseStack.mulPose(Axis.YP.rotationDegrees(VehicleVecUtils.getYRotFromVector(forward).toFloat()))
            poseStack.mulPose(Axis.XP.rotationDegrees(-VehicleVecUtils.getXRotFromVector(forward).toFloat()))
            // The existing model is four pixels (1/4 block) long before scaling.
            poseStack.scale(thickness, thickness, (used * 4.0).toFloat())
            instance.renderToBuffer(poseStack, vertices, packedLight, OverlayTexture.NO_OVERLAY,
                r, g, b, 0.65f * fade)
            poseStack.popPose()
            travelled += used
            from = end
            if (used < length) break
        }
    }

    override fun getBlockLightLevel(pEntity: ProjectileEntity, pPos: BlockPos): Int = 15

    companion object {
        val TEXTURE = loc("textures/bedrock/projectile/projectile.png")
    }
}
