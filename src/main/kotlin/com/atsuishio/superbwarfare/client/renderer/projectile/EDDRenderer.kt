package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.config.server.ExplosionConfig
import com.atsuishio.superbwarfare.entity.projectile.EDDEntity
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.core.Direction
import net.minecraft.resources.ResourceLocation

class EDDRenderer(renderManager: EntityRendererProvider.Context) : EntityRenderer<EDDEntity>(renderManager) {
    override fun render(
        entity: EDDEntity,
        yaw: Float,
        partialTick: Float,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int
    ) {
        val model = ProjectileModelReloadListener.getModel(MODEL) ?: return
        val bone = model.getBone("laser") ?: return

        poseStack.pushPose()

        val direction = entity.direction

        poseStack.mulPose(Axis.XP.rotationDegrees(180f))
        poseStack.mulPose(Axis.YN.rotationDegrees(direction.toYRot() + 180f))

        if (direction == Direction.EAST || direction == Direction.WEST) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180f))
        }

        if (!entity.isFacingLeft()) {
            poseStack.mulPose(Axis.ZP.rotationDegrees(180f))
        }

        bone.visible = entity.tickCount <= 20
        bone.zScale = ExplosionConfig.EDD_TRACE_RANGE.get().toFloat()

        model.renderToBuffer(
            poseStack,
            buffer.getBuffer(RenderType.entityCutout(this.getTextureLocation(entity))),
            packedLight,
            OverlayTexture.NO_OVERLAY
        )

        model.applyPose(model.bindPose)

        poseStack.popPose()
    }

    override fun getTextureLocation(entity: EDDEntity): ResourceLocation {
        return if (entity.uuid.leastSignificantBits % 191 == 0L) TEXTURE_ALTER else TEXTURE
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/projectile/edd.png")
        val TEXTURE_ALTER = loc("textures/bedrock/projectile/edd_alter.png")
        val MODEL = loc("models/bedrock/projectile/edd.geo.json")
    }
}