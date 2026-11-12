package com.atsuishio.superbwarfare.client.renderer.item

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.resource.model.ProjectileModelReloadListener
import com.github.mcmodderanchor.simplebedrockmodel.v1.client.renderer.BedrockModelRenderTypes
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.model.geom.EntityModelSet
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack

class NuclearBombItemRenderer(dispatcher: BlockEntityRenderDispatcher, models: EntityModelSet, modelId: String) :
    BlockEntityWithoutLevelRenderer(dispatcher, models) {
    private val modelLocation = loc("models/bedrock/projectile/$modelId.geo.json")
    private val textureLocation = loc("textures/bedrock/projectile/$modelId.png")

    override fun renderByItem(
        stack: ItemStack, context: ItemDisplayContext, pose: PoseStack,
        buffer: MultiBufferSource, light: Int, overlay: Int
    ) {
        // Resolve the model after resource loading, including subsequent resource reloads.
        val instance = ProjectileModelReloadListener.getModel(modelLocation)?.createInstance() ?: return
        pose.pushPose()
        pose.translate(0.5, 0.5, 0.5)
        instance.renderToBuffer(
            pose, buffer, RenderType.entityCutout(textureLocation),
            BedrockModelRenderTypes.polyMeshCutout(textureLocation), light, overlay
        )
        pose.popPose()
    }
}
