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
import net.minecraft.resources.ResourceLocation
import net.minecraft.util.RandomSource
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.level.block.state.BlockState

class ShelfBlockEntityRenderer : BlockEntityRenderer<ShelfBlockEntity> {
    private val random = RandomSource.create(0)

    override fun render(
        blockEntity: ShelfBlockEntity, partialTick: Float, pose: PoseStack,
        buffer: MultiBufferSource, light: Int, overlay: Int
    ) {
        val state = blockEntity.blockState
        renderModel(state, pose, buffer, light, overlay)
        val itemRenderer = Minecraft.getInstance().itemRenderer
        pose.pushPose()
        pose.translate(0.5, 0.0, 0.5)
        pose.mulPose(Axis.YP.rotationDegrees(180f - state.getValue(ShelfBlock.FACING).toYRot()))
        for (slot in 0..1) {
            val stack = blockEntity.getItem(slot)
            if (stack.isEmpty) continue
            val copies = displayCount(stack.count)
            val seed = blockEntity.blockPos.asLong().toInt() + slot
            random.setSeed(blockEntity.blockPos.asLong() + slot * 31L)
            pose.pushPose()
            pose.translate(0.0, (if (slot == 0) 6.0 else 14.0) / 16.0, -0.05)
            StaticGunRenderer.renderItem {
                val model = itemRenderer.getModel(stack, blockEntity.level, null, seed)
                repeat(copies) { copy ->
                    pose.pushPose()
                    transformItem(pose, copies, copy)
                    itemRenderer.render(stack, ItemDisplayContext.FIXED, false, pose, buffer, light, overlay, model)
                    pose.popPose()
                }
            }
            pose.popPose()
        }
        pose.popPose()
    }

    private fun displayCount(count: Int) = when {
        count <= 0 -> 0
        count == 1 -> 1
        count <= 16 -> 2
        count <= 32 -> 3
        count <= 48 -> 4
        else -> 5
    }

    private fun transformItem(pose: PoseStack, copies: Int, index: Int) {
        if (copies == 1) {
            pose.scale(0.5f, 0.5f, 0.5f)
            return
        }
        pose.translate(0.0, -0.015, 0.0)
        if (index > 0) {
            val xDirection = if (index % 2 == 0) 1.0 else -1.0
            val yDirection = if (index == 1 || index == 4) 1.0 else -1.0
            pose.translate(xDirection * (0.015 + random.nextFloat() * 0.01),
                yDirection * (0.006 + random.nextFloat() * 0.006), index * 0.035)
            pose.mulPose(Axis.YP.rotationDegrees((random.nextFloat() - 0.5f) * 16f))
            pose.mulPose(Axis.ZP.rotationDegrees((random.nextFloat() - 0.5f) * 12f))
        }
        pose.scale(0.44f, 0.44f, 0.44f)
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

    companion object {
        private val models = mutableMapOf<ResourceLocation, Model>()
        private val topTexture = loc("textures/bedrock/block/shelf_top.png")
        private val baseTexture = loc("textures/bedrock/block/shelf_base.png")

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
