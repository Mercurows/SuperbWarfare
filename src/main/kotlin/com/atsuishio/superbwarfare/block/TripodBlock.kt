package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.TripodBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class TripodBlock :
    BaseEntityBlock(
        Properties.of().sound(SoundType.METAL).strength(5.0f, 6.0f).noOcclusion().requiresCorrectToolForDrops()
    ) {

    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape {
        return SHAPE
    }

    override fun getVisualShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape {
        return Shapes.block()
    }

    override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.ENTITYBLOCK_ANIMATED
    }

    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity {
        return TripodBlockEntity(pPos, pState)
    }

    companion object {
        @JvmField
        val SHAPE: VoxelShape = Shapes.box(0.25, 0.0, 0.25, 0.75, 0.65625, 0.75)
    }
}
