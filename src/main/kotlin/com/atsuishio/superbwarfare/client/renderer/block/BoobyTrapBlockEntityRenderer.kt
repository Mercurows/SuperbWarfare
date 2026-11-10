package com.atsuishio.superbwarfare.client.renderer.block

import com.atsuishio.superbwarfare.block.BoobyTrapBlock
import com.atsuishio.superbwarfare.block.entity.BoobyTrapBlockEntity
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.util.RandomSource
import net.minecraft.world.level.BlockAndTintGetter
import net.minecraft.world.level.ColorResolver
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.client.RenderTypeHelper
import net.minecraftforge.client.model.data.ModelData

class BoobyTrapBlockEntityRenderer : BlockEntityRenderer<BoobyTrapBlockEntity> {
    override fun render(
        entity: BoobyTrapBlockEntity,
        partialTick: Float,
        pose: PoseStack,
        buffers: MultiBufferSource,
        light: Int,
        overlay: Int
    ) {
        if (!entity.blockState.getValue(BoobyTrapBlock.CAMOUFLAGED) || entity.blockState.getValue(BoobyTrapBlock.OFF)) return
        val state = entity.camouflage ?: return
        val world = entity.level ?: return
        val pos = entity.blockPos
        // World tessellation resolves biome tint per quad and samples neighboring light/AO.
        val view = object : BlockAndTintGetter {
            override fun getBlockState(position: BlockPos): BlockState {
                if (position == pos) return state
                val neighbor = world.getBlockState(position)
                if (neighbor.block is BoobyTrapBlock && neighbor.getValue(BoobyTrapBlock.CAMOUFLAGED) && !neighbor.getValue(
                        BoobyTrapBlock.OFF
                    )
                ) {
                    return (world.getBlockEntity(position) as? BoobyTrapBlockEntity)?.camouflage ?: neighbor
                }
                return neighbor
            }

            override fun getBlockEntity(position: BlockPos): BlockEntity? =
                if (position == pos) null else world.getBlockEntity(position)

            override fun getFluidState(position: BlockPos) = getBlockState(position).fluidState

            override fun getHeight() = world.height

            override fun getMinBuildHeight() = world.minBuildHeight

            override fun getShade(direction: Direction, shade: Boolean) = world.getShade(direction, shade)

            override fun getShade(normalX: Float, normalY: Float, normalZ: Float, shade: Boolean) =
                world.getShade(normalX, normalY, normalZ, shade)

            override fun getLightEngine() = world.lightEngine

            override fun getBlockTint(position: BlockPos, resolver: ColorResolver) =
                world.getBlockTint(position, resolver)

            override fun getModelDataManager() = world.modelDataManager
        }
        val dispatcher = Minecraft.getInstance().blockRenderer
        val model = dispatcher.getBlockModel(state)
        val data = model.getModelData(view, pos, state, ModelData.EMPTY)
        val seed = state.getSeed(pos)
        for (layer in model.getRenderTypes(state, RandomSource.create(seed), data)) {
            // Tessellation already applies directional shading; entity shaders would apply it a second time.
            val buffer = buffers.getBuffer(RenderTypeHelper.getMovingBlockRenderType(layer))
            dispatcher.modelRenderer.tesselateBlock(
                view, model, state, pos, pose, buffer, true,
                RandomSource.create(seed), seed, overlay, data, layer
            )
        }
    }
}
