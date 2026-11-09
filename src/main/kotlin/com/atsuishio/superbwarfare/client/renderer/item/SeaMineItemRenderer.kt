package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.item.projectile.SeaMineItem
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
import net.minecraft.client.renderer.texture.OverlayTexture
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class SeaMineItemRenderer(dispatcher: BlockEntityRenderDispatcher, models: EntityModelSet) :
    BlockEntityWithoutLevelRenderer(dispatcher, models) {
    override fun renderByItem(
        stack: ItemStack, context: ItemDisplayContext, pose: PoseStack,
        buffer: MultiBufferSource, light: Int, overlay: Int
    ) {
        if (stack.item !is SeaMineItem) return
        val instance = modelInstance ?: return
        pose.pushPose()

        pose.translate(0.5, 0.5, 0.5)

        instance.renderToBuffer(
            pose,
            buffer,
            RenderType.entityCutout(TEXTURE),
            BedrockModelRenderTypes.polyMeshCutout(TEXTURE),
            light,
            OverlayTexture.NO_OVERLAY
        )

        pose.popPose()
    }

    companion object {
        val MODEL = Mod.loc("models/bedrock/projectile/sea_mine.geo.json")
        val TEXTURE = Mod.loc("textures/bedrock/projectile/sea_mine.png")
        val modelInstance = ProjectileModelReloadListener.getModel(MODEL)?.createInstance()
    }
}
