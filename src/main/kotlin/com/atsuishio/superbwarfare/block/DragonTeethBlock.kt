package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.Mob
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.level.pathfinder.BlockPathTypes
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class DragonTeethBlock : Block(
    Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE)
        .strength(25f, 500f)
        .requiresCorrectToolForDrops().pushReaction(PushReaction.BLOCK).noOcclusion()
        .isRedstoneConductor { _, _, _ -> false }
) {
    init {
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false))
    }

    override fun propagatesSkylightDown(state: BlockState, reader: BlockGetter, pos: BlockPos): Boolean {
        return true
    }

    override fun getLightBlock(state: BlockState, worldIn: BlockGetter, pos: BlockPos): Int {
        return 0
    }

    override fun getVisualShape(
        state: BlockState,
        world: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return Shapes.empty()
    }

    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        return Shapes.or(box(2.0, 0.0, 2.0, 14.0, 25.0, 14.0))
    }

    override fun getBlockPathType(
        state: BlockState?,
        world: BlockGetter?,
        pos: BlockPos?,
        entity: Mob?
    ): BlockPathTypes {
        return BlockPathTypes.LAVA
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(WATERLOGGED)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val flag = context.level.getFluidState(context.clickedPos).type === Fluids.WATER
        return this.defaultBlockState().setValue(WATERLOGGED, flag)
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)
    }

    override fun updateShape(
        state: BlockState,
        facing: Direction,
        facingState: BlockState,
        world: LevelAccessor,
        currentPos: BlockPos,
        facingPos: BlockPos
    ): BlockState {
        if (state.getValue(WATERLOGGED)) {
            world.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(world))
        }
        return super.updateShape(state, facing, facingState, world, currentPos, facingPos)
    }

    override fun stepOn(pLevel: Level, pPos: BlockPos, pState: BlockState, pEntity: Entity) {
        super.stepOn(pLevel, pPos, pState, pEntity)

        if (pEntity is VehicleEntity) {
            pEntity.setDeltaMovement(pEntity.deltaMovement.multiply(0.05, 0.05, 0.05))
        }
    }

    companion object {
        @JvmField
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED
    }
}

