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

class DecoyGrenadeItemRenderer(dispatcher: BlockEntityRenderDispatcher, models: EntityModelSet) :
    BlockEntityWithoutLevelRenderer(dispatcher, models) {
    private var model = ProjectileModelReloadListener.getModel(MODEL)
    private var instance = model?.createInstance()

    override fun renderByItem(
        stack: ItemStack, context: ItemDisplayContext, pose: PoseStack,
        buffer: MultiBufferSource, light: Int, overlay: Int
    ) {
        val currentModel = ProjectileModelReloadListener.getModel(MODEL)
        if (model !== currentModel) {
            model = currentModel
            instance = currentModel?.createInstance()
        }
        val instance = instance ?: return
        pose.pushPose()
        pose.translate(0.5, 0.5, 0.5)
        instance.resetPose()
        instance.renderToBuffer(
            pose, buffer, RenderType.entityCutout(TEXTURE),
            BedrockModelRenderTypes.polyMeshCutout(TEXTURE), light, overlay
        )
        pose.popPose()
    }

    companion object {
        val MODEL = loc("models/bedrock/projectile/decoy_grenade.geo.json")
        val TEXTURE = loc("textures/bedrock/projectile/decoy_grenade.png")
    }
}
