package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.container.ContainerBlockItem
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class ContainerBlockItemRenderer(dispatcher: BlockEntityRenderDispatcher, set: EntityModelSet) :
    BlockEntityWithoutLevelRenderer(dispatcher, set) {
    // TODO 把模型位置调对，再加上GUI禁用此渲染
    override fun renderByItem(
        stack: ItemStack,
        transformType: ItemDisplayContext,
        poseStack: PoseStack,
        bufferSource: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if (stack.item !is ContainerBlockItem) return

        val model = BlockModelReloadListener.getModel(MODEL) ?: return

        poseStack.pushPose()

//        poseStack.translate(0.8, 0.8, 0.4)

        model.renderToBuffer(
            poseStack,
            bufferSource.getBuffer(RenderType.entityCutout(TEXTURE)),
            packedLight,
            packedOverlay
        )

        model.applyPose(model.bindPose)

        poseStack.popPose()
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/block/container.png")
        val MODEL = loc("models/bedrock/block/container.geo.json")
    }
}
