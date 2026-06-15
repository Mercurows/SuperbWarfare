package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.item.projectile.Tm62Item
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
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
        val model = ProjectileModelReloadListener.getModel(MODEL) ?: return
        poseStack.pushPose()

        poseStack.translate(0.5f, 0.5f, 0.5f)

        model.applyPose(model.bindPose)

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
        val MODEL = loc("models/bedrock/projectile/tm_62.geo.json")
    }
}
