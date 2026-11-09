package com.atsuishio.superbwarfare.client.renderer.projectile

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.entity.projectile.SeaMineEntity
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.atsuishio.superbwarfare.tools.mc
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.entity.EntityRenderer
import net.minecraft.client.renderer.entity.EntityRendererProvider
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.Mth
import net.minecraft.world.level.block.Blocks
import kotlin.math.ceil

class SeaMineRenderer(context: EntityRendererProvider.Context) : EntityRenderer<SeaMineEntity>(context) {
    private var model = ProjectileModelReloadListener.getModel(MODEL)
    private var modelInstance = model?.createInstance()

    override fun getTextureLocation(entity: SeaMineEntity): ResourceLocation {
        val uuid = entity.getUUID()
        return if (uuid.leastSignificantBits % 419 == 0L) {
            TEXTURE_DS
        } else {
            TEXTURE
        }
    }

    override fun render(
        entity: SeaMineEntity, yaw: Float, partialTick: Float, pose: PoseStack,
        buffer: MultiBufferSource, light: Int
    ) {
        val currentModel = ProjectileModelReloadListener.getModel(MODEL)
        if (model !== currentModel) {
            model = currentModel
            modelInstance = currentModel?.createInstance()
        }
        val instance = modelInstance ?: return
        instance.resetPose()

        val texture = getTextureLocation(entity)
        instance.renderToBuffer(
            pose, buffer, RenderType.entityCutout(texture),
            BedrockModelRenderTypes.polyMeshCutout(texture), light, OverlayTexture.NO_OVERLAY
        )

        val anchor = entity.anchor
        if (anchor != null && !entity.released && entity.chainLength > 0) {
            pose.pushPose()
            val x = Mth.lerp(partialTick.toDouble(), entity.xo, entity.x)
            val y = Mth.lerp(partialTick.toDouble(), entity.yo, entity.y)
            val z = Mth.lerp(partialTick.toDouble(), entity.zo, entity.z)
            // The model's bottom attachment sits above the entity origin.
            val length = (y - anchor.y).coerceIn(0.0, entity.chainLength.toDouble()) + 3.0 / 16.0
            pose.translate(anchor.x - x, anchor.y - y, anchor.z - z)
            for (segment in 0 until ceil(length).toInt()) {
                pose.pushPose()
                pose.translate(0.0, segment.toDouble(), 0.0)
                pose.scale(0.5f, (length - segment).coerceAtMost(1.0).toFloat(), 0.5f)
                pose.translate(-0.5, 0.0, -0.5)
                mc.blockRenderer.renderSingleBlock(
                    Blocks.CHAIN.defaultBlockState(), pose, buffer, light, OverlayTexture.NO_OVERLAY
                )
                pose.popPose()
            }
            pose.popPose()
        }
        super.render(entity, yaw, partialTick, pose, buffer, light)
    }

    companion object {
        val MODEL = Mod.loc("models/bedrock/projectile/sea_mine.geo.json")
        val TEXTURE = Mod.loc("textures/bedrock/projectile/sea_mine.png")
        val TEXTURE_DS = Mod.loc("textures/bedrock/projectile/sea_mine_ds.png")
    }
}