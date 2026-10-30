package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.ResonanceBaseBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.phys.shapes.BooleanOp
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

@Suppress("OVERRIDE_DEPRECATION")
class ResonanceBaseBlock : BaseEntityBlock(
    Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL)
        .strength(4f, 6f).requiresCorrectToolForDrops().lightLevel { 5 }.noOcclusion()
        .pushReaction(PushReaction.BLOCK)
) {
    init {
        registerDefaultState(stateDefinition.any().setValue(PART_X, 1).setValue(PART_Z, 1))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(PART_X, PART_Z)
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity? =
        if (isCenter(state)) ResonanceBaseBlockEntity(pos, state) else null

    override fun getRenderShape(state: BlockState) = RenderShape.ENTITYBLOCK_ANIMATED

    override fun getShape(state: BlockState, level: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape =
        SHAPES[state.getValue(PART_X) * 3 + state.getValue(PART_Z)]

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val level = context.level
        for (x in 0..2) for (z in 0..2) {
            val pos = context.clickedPos.offset(x - 1, 0, z - 1)
            val state = defaultBlockState().setValue(PART_X, x).setValue(PART_Z, z)
            if (level.isOutsideBuildHeight(pos) || !level.hasChunkAt(pos)
                || !level.worldBorder.isWithinBounds(pos)
                || !level.getBlockState(pos).canBeReplaced(context)
                || !level.isUnobstructed(state, pos, CollisionContext.empty())
                || context.player?.mayUseItemAt(pos, context.clickedFace, context.itemInHand) == false
            ) return null
        }
        return defaultBlockState()
    }

    override fun setPlacedBy(level: Level, pos: BlockPos, state: BlockState, placer: LivingEntity?, stack: ItemStack) {
        super.setPlacedBy(level, pos, state, placer, stack)
        if (level.isClientSide) return
        for (x in 0..2) for (z in 0..2) {
            if (x == 1 && z == 1) continue
            level.setBlock(pos.offset(x - 1, 0, z - 1), state.setValue(PART_X, x).setValue(PART_Z, z), UPDATE_ALL)
        }
    }

    override fun onRemove(state: BlockState, level: Level, pos: BlockPos, newState: BlockState, moving: Boolean) {
        if (!state.`is`(newState.block) && !level.isClientSide) {
            val center = centerPos(pos, state)
            // Removing the other parts without drops leaves the originally broken part's
            // normal loot table responsible for the single item (also for explosions).
            for (x in 0..2) for (z in 0..2) {
                val otherPos = center.offset(x - 1, 0, z - 1)
                if (otherPos == pos) continue
                val other = level.getBlockState(otherPos)
                if (other.`is`(this) && centerPos(otherPos, other) == center) {
                    level.removeBlock(otherPos, false)
                }
            }
        }
        super.onRemove(state, level, pos, newState, moving)
    }

    companion object {
        val PART_X: IntegerProperty = IntegerProperty.create("part_x", 0, 2)
        val PART_Z: IntegerProperty = IntegerProperty.create("part_z", 0, 2)

        fun isCenter(state: BlockState) = state.getValue(PART_X) == 1 && state.getValue(PART_Z) == 1
        fun centerPos(pos: BlockPos, state: BlockState): BlockPos =
            pos.offset(1 - state.getValue(PART_X), 0, 1 - state.getValue(PART_Z))

        // The model's stepped silhouette, relative to the center block's corner.
        private val COMPLETE_SHAPE = Shapes.or(
            box(-13.0, 0.0, -13.0, 29.0, 2.0, 29.0),
            box(-12.0, 2.0, -12.0, 28.0, 3.0, 28.0),
            box(-10.5, 3.0, -10.5, 26.5, 9.5, 26.5),
            box(-12.0, 9.5, -12.0, 28.0, 11.0, 28.0),
            box(-14.0, 11.0, -14.0, 30.0, 12.0, 30.0),
            box(-16.0, 12.0, -16.0, 32.0, 16.0, 32.0),
            *listOf(-11.25, 25.25).flatMap { x ->
                listOf(-11.25, 25.25).map { z -> box(x, 3.0, z, x + 2, 9.5, z + 2) }
            }.toTypedArray()
        )
        private val SHAPES = Array(9) { index ->
            Shapes.join(
                COMPLETE_SHAPE.move((1 - index / 3).toDouble(), 0.0, (1 - index % 3).toDouble()),
                Shapes.block(), BooleanOp.AND
            ).optimize()
        }
    }
}
