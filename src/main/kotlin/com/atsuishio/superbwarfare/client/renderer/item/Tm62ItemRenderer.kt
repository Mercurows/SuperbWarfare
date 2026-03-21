package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.projectile.Tm62Item
import com.atsuishio.superbwarfare.resource.BedrockModelLoader
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class Tm62ItemRenderer(dispatcher: BlockEntityRenderDispatcher, set: EntityModelSet) :
    BlockEntityWithoutLevelRenderer(dispatcher, set) {
    override fun renderByItem(
        stack: ItemStack,
        displayContext: ItemDisplayContext,
        poseStack: PoseStack,
        buffer: MultiBufferSource,
        packedLight: Int,
        packedOverlay: Int
    ) {
        if (stack.item !is Tm62Item) return
        val model = BedrockModelLoader.getModel(BedrockModelLoader.TM_62_MODEL) ?: return
        poseStack.pushPose()

        if (displayContext == ItemDisplayContext.GUI) {
            poseStack.translate(0.45f, 0.45f, 0f)
            poseStack.mulPose(Axis.XP.rotationDegrees(55f))
            poseStack.mulPose(Axis.YP.rotationDegrees(30f))
            poseStack.mulPose(Axis.ZP.rotationDegrees(-35f))
            poseStack.scale(1.9f, 1.9f, 1.9f)
        } else if (displayContext.firstPerson() || displayContext == ItemDisplayContext.GROUND) {
            poseStack.translate(0.5f, 0.5f, 0.45f)
        } else {
            poseStack.translate(0.5f, 0.35f, 0.35f)
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
        val TEXTURE = loc("textures/bedrock/projectile/tm_62.png")
    }
}
