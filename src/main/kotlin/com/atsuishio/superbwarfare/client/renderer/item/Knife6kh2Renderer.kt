package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.weapon.Knife6kh2Item
import com.atsuishio.superbwarfare.resource.model.ItemModelReloadListener
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

class Knife6kh2Renderer(dispatcher: BlockEntityRenderDispatcher, set: EntityModelSet) :
    BlockEntityWithoutLevelRenderer(dispatcher, set) {
    override fun renderByItem(
        stack: ItemStack,
        displayContext: ItemDisplayContext,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if (stack.item !is Knife6kh2Item) return
        val instance = modelInstance ?: return
        poseStack.pushPose()

        poseStack.translate(0.5f, 0.5f, 0.5f)

        instance.renderToBuffer(
            poseStack,
            buffer,
            RenderType.entityCutout(TEXTURE),
            BedrockModelRenderTypes.polyMeshCutout(TEXTURE),
            packedLight,
            OverlayTexture.NO_OVERLAY
        )

        poseStack.popPose()
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/item/knife_6kh2.png")
        val MODEL = loc("models/bedrock/item/knife_6kh2.geo.json")
        val modelInstance = ItemModelReloadListener.getModel(MODEL)?.createInstance()
    }
}
