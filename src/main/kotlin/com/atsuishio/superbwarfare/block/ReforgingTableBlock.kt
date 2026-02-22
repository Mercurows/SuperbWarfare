package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.inventory.menu.ReforgingTableMenu
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.MenuProvider
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.ContainerLevelAccess
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument
import net.minecraft.world.level.material.FluidState
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class ReforgingTableBlock : Block(
    Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.STONE).strength(2f)
        .lightLevel { _ -> 4 }
        .hasPostProcess { _, _, _ -> true }
        .emissiveRendering { _, _, _ -> true }
) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(WATERLOGGED, false)
        )
    }

    override fun use(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pPlayer: Player,
        pHand: InteractionHand,
        pHit: BlockHitResult
    ): InteractionResult {
        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS
        } else {
            pPlayer.openMenu(pState.getMenuProvider(pLevel, pPos))
            return InteractionResult.CONSUME
        }
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block?, BlockState?>) {
        builder.add(FACING, WATERLOGGED)
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

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val flag = context.level.getFluidState(context.clickedPos).type === Fluids.WATER
        return this.defaultBlockState()
            .setValue(FACING, context.horizontalDirection.opposite)
            .setValue(WATERLOGGED, flag)
    }

    override fun getFluidState(state: BlockState): FluidState {
        return if (state.getValue(WATERLOGGED)) Fluids.WATER.getSource(false) else super.getFluidState(state)
    }

    override fun rotate(state: BlockState, rot: Rotation): BlockState {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)))
    }

    override fun mirror(state: BlockState, mirrorIn: Mirror): BlockState {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)))
    }

    override fun getShape(state: BlockState, world: BlockGetter, pos: BlockPos, context: CollisionContext): VoxelShape {
        val direction = state.getValue(FACING)
        return if (direction == Direction.NORTH || direction == Direction.SOUTH) {
            Shapes.or(
                box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0),
                box(1.0, 1.0, 1.0, 15.0, 3.0, 15.0),
                box(5.0, 4.0, 6.5, 11.0, 16.6, 9.5)
            )
        } else {
            Shapes.or(
                box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0),
                box(1.0, 1.0, 1.0, 15.0, 3.0, 15.0),
                box(6.5, 4.0, 5.0, 9.5, 16.6, 11.0)
            )
        }
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

    override fun getMenuProvider(pState: BlockState, pLevel: Level, pPos: BlockPos): MenuProvider? {
        return SimpleMenuProvider({ i, inventory, _ ->
            ReforgingTableMenu(
                i,
                inventory,
                ContainerLevelAccess.create(pLevel, pPos)
            )
        }, CONTAINER_TITLE)
    }

    companion object {
        @JvmField
        val WATERLOGGED: BooleanProperty = BlockStateProperties.WATERLOGGED

        @JvmField
        val FACING: DirectionProperty = HorizontalDirectionalBlock.FACING
        private val CONTAINER_TITLE: Component = Component.translatable("container.superbwarfare.reforging_table")
    }
}
