package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.client.ClientRenderHandler
import com.atsuishio.superbwarfare.entity.projectile.SmallCannonShellEntity
import com.atsuishio.superbwarfare.entity.vehicle.utils.VehicleVecUtils
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
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
import net.minecraft.util.Mth

class SmallCannonShellEntityRenderer(manager: EntityRendererProvider.Context) :
    EntityRenderer<SmallCannonShellEntity>(manager) {
    override fun getTextureLocation(pEntity: SmallCannonShellEntity) = loc("textures/entity/empty.png")

    override fun shouldRender(
        pLivingEntity: SmallCannonShellEntity,
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
        entity: SmallCannonShellEntity,
        entityYaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        val model = ProjectileModelReloadListener.getModel(MODEL) ?: return
        val eyePos = localPlayer?.eyePosition ?: return

        poseStack.pushPose()

        ClientRenderHandler.transformVirtualRenderPosition(poseStack, entity, partialTick)

        val width = 0.3f
        val position = entity.getPosition(partialTick)
        val distance = position.distanceTo(eyePos)
        val length = 0.7 * entity.deltaMovement.length() * Mth.clamp(0.75 * entity.tickCount, 0.0, 1.0)

        poseStack.mulPose(Axis.YP.rotationDegrees(VehicleVecUtils.getYRotFromVector(entity.deltaMovement).toFloat()))
        poseStack.mulPose(Axis.XP.rotationDegrees(-VehicleVecUtils.getXRotFromVector(entity.deltaMovement).toFloat()))
        poseStack.scale(width, width, length.toFloat())

        if (entity.tickCount >= 5 || distance > 5.0) {
            val type = RenderType.energySwirl(TEXTURE, 15.0f, 15.0f)
            model.renderToBuffer(
                poseStack,
                buffer.getBuffer(type),
                packedLight,
                OverlayTexture.NO_OVERLAY,
                1.0f,
                222f / 255f,
                39f / 255f,
                1.0f
            )
        }

        poseStack.popPose()
    }

    override fun getBlockLightLevel(pEntity: SmallCannonShellEntity, pPos: BlockPos): Int = 15

    companion object {
        val TEXTURE = loc("textures/bedrock/projectile/small_cannon_shell.png")
        val MODEL = loc("models/bedrock/projectile/small_cannon_shell.geo.json")
    }
}