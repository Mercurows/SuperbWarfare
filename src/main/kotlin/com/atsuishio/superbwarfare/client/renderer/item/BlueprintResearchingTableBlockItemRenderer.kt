package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.blockitem.BlueprintResearchTableBlockItem
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class BlueprintResearchingTableBlockItemRenderer(dispatcher: BlockEntityRenderDispatcher, set: EntityModelSet) :
    BlockEntityWithoutLevelRenderer(dispatcher, set) {
    override fun renderByItem(
        stack: ItemStack,
        displayContext: ItemDisplayContext,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if (stack.item !is BlueprintResearchTableBlockItem) return
        val model = BlockModelReloadListener.getModel(MODEL) ?: return

        poseStack.pushPose()

        poseStack.translate(0.5f, 0.15f, 0.5f)

        if (displayContext == ItemDisplayContext.GUI) {
            poseStack.scale(0.365f, 0.365f, 0.365f)
            poseStack.translate(-0.3f, 0.0f, 0.5f)
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0f))
            poseStack.mulPose(Axis.YN.rotationDegrees(45.0f))
        } else {
            poseStack.scale(0.35f, 0.35f, 0.35f)
            poseStack.mulPose(Axis.YN.rotationDegrees(180.0f))

            if (displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ||
                displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND
            ) {
                poseStack.translate(0f, 0f, 1f)
            }
        }

        model.renderToBuffer(
            poseStack,
            buffer.getBuffer(RenderType.entityCutout(TEXTURE)),
            packedLight,
            packedOverlay
        )

        poseStack.popPose()
    }

    companion object {
        val TEXTURE = loc("textures/bedrock/block/blueprint_research_table.png")
        val MODEL = loc("models/bedrock/block/blueprint_research_table.geo.json")
    }
}