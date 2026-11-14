package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.block.ShelfBlock
import com.atsuishio.superbwarfare.block.entity.ShelfBlockEntity
import com.atsuishio.superbwarfare.client.renderer.gun.StaticGunRenderer
import com.atsuishio.superbwarfare.resource.model.BlockModelReloadListener
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.baked.BakedBedrockModel
import com.github.mcmodderanchor.simplebedrockmodel.v2.common.model.runtime.BakedModelInstance
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.client.resources.model.ModelResourceLocation
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.event.ModelEvent
import net.minecraftforge.client.model.BakedModelWrapper

class ShelfBlockEntityRenderer : BlockEntityRenderer<ShelfBlockEntity> {
    override fun render(
        blockEntity: ShelfBlockEntity, partialTick: Float, pose: PoseStack,
        buffer: MultiBufferSource, light: Int, overlay: Int
    ) {
        val state = blockEntity.blockState
        renderModel(state, pose, buffer, light, overlay)
        pose.pushPose()
        pose.translate(0.5, 0.0, 0.5)
        pose.mulPose(Axis.YP.rotationDegrees(180f - state.getValue(ShelfBlock.FACING).toYRot()))
        for (slot in 0..1) {
            val stack = blockEntity.getItem(slot)
            if (stack.isEmpty) continue
            pose.pushPose()
            pose.translate(0.0, (if (slot == 0) 6.0 else 14.0) / 16.0, -0.05)
            pose.scale(0.5f, 0.5f, 0.5f)
            StaticGunRenderer.renderItem {
                Minecraft.getInstance().itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay,
                    pose, buffer, blockEntity.level, blockEntity.blockPos.asLong().toInt() + slot)
            }
            pose.popPose()
        }
        pose.popPose()
    }

    private class Model {
        private var model: BakedBedrockModel? = null
        private var instance: BakedModelInstance? = null

        fun getInstance(location: ResourceLocation): BakedModelInstance? {
            val current = BlockModelReloadListener.getModel(location)
            if (current !== model) {
                model = current
                instance = current?.createInstance()
            }
            return instance
        }
    }

    private class ItemModel(original: BakedModel) : BakedModelWrapper<BakedModel>(original) {
        override fun isCustomRenderer() = true

        override fun applyTransform(context: ItemDisplayContext, pose: PoseStack, leftHand: Boolean): BakedModel {
            super.applyTransform(context, pose, leftHand)
            return this
        }
    }

    companion object {
        private val models = mutableMapOf<ResourceLocation, Model>()
        private val topTexture = loc("textures/bedrock/block/shelf_top.png")
        private val baseTexture = loc("textures/bedrock/block/shelf_base.png")

        fun modifyItemModel(event: ModelEvent.ModifyBakingResult) {
            val location = ModelResourceLocation(loc("shelf"), "inventory")
            val model = event.models[location] ?: return
            event.models[location] = ItemModel(model)
        }

        fun renderModel(state: BlockState, pose: PoseStack, buffer: MultiBufferSource, light: Int, overlay: Int) {
            val base = state.getValue(ShelfBlock.BASE)
            val part = state.getValue(ShelfBlock.PART)
            val modelName = if (part == ShelfBlock.Part.MIDDLE) "shelf_base_middle"
                else "shelf_${if (base) "base" else "top"}_${part.serializedName}"
            val location = loc("models/bedrock/block/$modelName.geo.json")
            val instance = models.getOrPut(location) { Model() }.getInstance(location) ?: return
            pose.pushPose()
            pose.translate(0.5, 0.0, 0.5)
            pose.mulPose(Axis.YP.rotationDegrees(180f - state.getValue(ShelfBlock.FACING).toYRot()))
            instance.resetPose()
            instance.renderToBuffer(pose, buffer.getBuffer(RenderType.entityCutout(if (base) baseTexture else topTexture)),
                light, overlay)
            pose.popPose()
        }
    }
}
