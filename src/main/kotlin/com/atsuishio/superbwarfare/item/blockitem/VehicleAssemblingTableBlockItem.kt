package com.atsuishio.superbwarfare.item.blockitem

import com.atsuishio.superbwarfare.block.VehicleAssemblingTableBlock
import com.atsuishio.superbwarfare.block.property.BlockPart
import com.atsuishio.superbwarfare.client.renderer.item.VehicleAssemblingTableBlockItemRenderer
import com.atsuishio.superbwarfare.init.ModBlocks
import com.atsuishio.superbwarfare.tools.mc
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraftforge.client.extensions.common.IClientItemExtensions
import java.util.function.Consumer

class VehicleAssemblingTableBlockItem : BlockItem(ModBlocks.VEHICLE_ASSEMBLING_TABLE.get(), Properties()) {

    // 多方块额外碰撞检测
    override fun canPlace(context: BlockPlaceContext, state: BlockState): Boolean {
        val facing = state.getValue(VehicleAssemblingTableBlock.FACING)
        val initialPos = findInitialPos(context, context.clickedPos, facing) ?: return false

        val player = context.player
        val collisionContext = if (player == null) CollisionContext.empty() else CollisionContext.of(player)

        // 检测是否所有位置都不会被实体挡住
        for (blockPart in BlockPart.entries) {
            val blockPos = blockPart.relative(initialPos, facing)
            if (!context.level.isUnobstructed(state, blockPos, collisionContext)) {
                return false
            }
        }

        return super.canPlace(context, state)
    }

    override fun initializeClient(consumer: Consumer<IClientItemExtensions?>) {
        super.initializeClient(consumer)
        consumer.accept(object : IClientItemExtensions {
            private var renderer: BlockEntityWithoutLevelRenderer? = null

            override fun getCustomRenderer(): BlockEntityWithoutLevelRenderer {
                if (renderer == null) {
                    renderer = VehicleAssemblingTableBlockItemRenderer(mc.blockEntityRenderDispatcher, mc.entityModels)
                }
                return renderer!!
            }
        })
    }

    companion object {
        // 根据当前状态尝试找到合适的初始放置位置
        fun findInitialPos(context: BlockPlaceContext, currentPos: BlockPos, facing: Direction): BlockPos? {
            var availablePart: BlockPart? = null
            for (part in BlockPart.entries) {
                val placePos = part.relativeNegative(currentPos, facing)
                if (canPlace(context, placePos, facing)) {
                    availablePart = part
                    break
                }
            }

            if (availablePart == null) return null
            return availablePart.relativeNegative(currentPos, facing)
        }

        fun canPlace(context: BlockPlaceContext, pos: BlockPos, direction: Direction): Boolean {
            for (part in BlockPart.entries) {
                val detectPos = part.relative(pos, direction)
                if (!context.level.getBlockState(detectPos).canBeReplaced(context)) {
                    return false
                }
            }
            return true
        }

        fun canPlace(level: Level, pos: BlockPos, direction: Direction, skipPos: BlockPos?): Boolean {
            for (part in BlockPart.entries) {
                val detectPos = part.relative(pos, direction)
                if (detectPos == skipPos) continue

                if (!level.getBlockState(detectPos).canBeReplaced()) {
                    return false
                }
            }
            return true
        }
    }
}
