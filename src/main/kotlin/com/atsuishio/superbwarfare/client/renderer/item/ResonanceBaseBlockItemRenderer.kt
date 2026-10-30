package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class ResonanceBaseBlockItemRenderer(dispatcher: BlockEntityRenderDispatcher, set: EntityModelSet) :
    BlockEntityWithoutLevelRenderer(dispatcher, set) {
    private var model = BlockModelReloadListener.getModel(MODEL)
    private var instance = model?.createInstance()

    override fun renderByItem(
        stack: ItemStack, displayContext: ItemDisplayContext, poseStack: PoseStack,
        buffer: MultiBufferSource, packedLight: Int, packedOverlay: Int
    ) {
        val current = BlockModelReloadListener.getModel(MODEL)
        if (model !== current) {
            model = current
            instance = current?.createInstance()
        }
        val instance = instance ?: return
        poseStack.pushPose()
        // Item display transforms rotate around (0.5, 0.5, 0.5). Center the
        // three-block model there; context-specific angles/scales live in JSON.
        poseStack.translate(0.5, 0.0, 0.5)
        instance.resetPose()
        instance.renderToBuffer(poseStack, buffer.getBuffer(RenderType.entityCutout(TEXTURE)), packedLight, packedOverlay)
        poseStack.popPose()
    }

    companion object {
        private val MODEL = loc("models/bedrock/block/resonance_base.geo.json")
        private val TEXTURE = loc("textures/bedrock/block/resonance_base.png")
    }
}
