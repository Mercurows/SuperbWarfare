package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.*
import net.minecraft.world.level.material.PushReaction
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class BlueprintResearchTableBlock :
    BaseEntityBlock(Properties.of().strength(2f).pushReaction(PushReaction.BLOCK)) {
    companion object {
        @JvmField
        val PART: EnumProperty<BedPart> = BlockStateProperties.BED_PART

        @JvmField
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING

        // 虽然这个是enabled，但是应该反转取值
        @JvmField
        val ENABLED: BooleanProperty = BlockStateProperties.ENABLED

        fun oppositeDirection(part: BedPart, direction: Direction): Direction =
            if (part == BedPart.FOOT) direction else direction.opposite
    }

    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(PART, BedPart.FOOT)
                .setValue(FACING, Direction.NORTH)
                .setValue(ENABLED, true)
        )
    }

    override fun getShape(
        state: BlockState,
        level: BlockGetter,
        pos: BlockPos,
        context: CollisionContext
    ): VoxelShape {
        return if (state.getValue(PART) == BedPart.FOOT) {
            when (state.getValue(FACING)) {
                Direction.SOUTH -> box(0.0, 0.0, 1.0, 16.0, 16.0, 16.0)
                Direction.EAST -> box(1.0, 0.0, 0.0, 16.0, 16.0, 16.0)
                Direction.WEST -> box(0.0, 0.0, 0.0, 15.0, 16.0, 16.0)
                else -> box(0.0, 0.0, 0.0, 16.0, 16.0, 15.0)
            }
        } else {
            when (state.getValue(FACING)) {
                Direction.SOUTH -> box(0.0, 0.0, 0.0, 16.0, 16.0, 15.0)
                Direction.EAST -> box(0.0, 0.0, 0.0, 15.0, 16.0, 16.0)
                Direction.WEST -> box(1.0, 0.0, 0.0, 16.0, 16.0, 16.0)
                else -> box(0.0, 0.0, 1.0, 16.0, 16.0, 16.0)
            }
        }
    }

    override fun isPathfindable(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pType: PathComputationType
    ): Boolean = false

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(PART, FACING, ENABLED)
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
            this.openContainer(pLevel, pPos, pState, pPlayer)
            return InteractionResult.CONSUME
        }
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: BlockGetter?,
        pTooltip: MutableList<Component>,
        pFlag: TooltipFlag
    ) {
        pTooltip.add(
            Component.translatable("des.superbwarfare.blueprint_research_table_1").withStyle(ChatFormatting.GRAY)
        )
        pTooltip.add(
            Component.translatable("des.superbwarfare.blueprint_research_table_2").withStyle(ChatFormatting.GRAY)
        )
    }

    private fun openContainer(level: Level, pos: BlockPos, state: BlockState, player: Player) {
        val entity = level.getBlockEntity(this.getRootPos(pos, state)) as? BlueprintResearchTableBlockEntity ?: return
        player.openMenu(entity)
    }

    override fun onPlace(pState: BlockState, pLevel: Level, pPos: BlockPos, pOldState: BlockState, pIsMoving: Boolean) {
        if (!pOldState.`is`(pState.block)) {
            this.checkPoweredState(pLevel, pPos, pState, 2)
        }
    }

    override fun newBlockEntity(
        pPos: BlockPos,
        pState: BlockState
    ): BlockEntity {
        return BlueprintResearchTableBlockEntity(pPos, pState)
    }

    override fun <T : BlockEntity?> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T?>
    ): BlockEntityTicker<T?>? {
        if (!pLevel.isClientSide) {
            return createTickerHelper(
                pBlockEntityType,
                ModBlockEntities.BLUEPRINT_RESEARCH_TABLE.get(),
                BlueprintResearchTableBlockEntity::serverTick
            )
        }
        return null
    }

    override fun onRemove(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pNewState: BlockState,
        pMovedByPiston: Boolean
    ) {
        if (!pState.`is`(pNewState.block)) {
            val entity = pLevel.getBlockEntity(pPos)
            if (entity is BlueprintResearchTableBlockEntity) {
                if (pLevel is ServerLevel) {
                    Containers.dropContents(pLevel, pPos, entity)
                }
                pLevel.updateNeighbourForOutputSignal(pPos, this)
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pMovedByPiston)
    }

    /**
     * Code based on TaC-Z
     */
    fun getRootPos(pos: BlockPos, state: BlockState): BlockPos {
        return if (state.getValue(PART) == BedPart.FOOT) pos else pos.relative(
            oppositeDirection(
                BedPart.HEAD,
                state.getValue(FACING)
            )
        )
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val direction = context.horizontalDirection.getClockWise()
        val clickedPos = context.clickedPos
        val relative = clickedPos.relative(direction)
        val level = context.level
        return if (level.getBlockState(relative).canBeReplaced(context) && level.worldBorder.isWithinBounds(relative))
            this.defaultBlockState().setValue(FACING, direction) else null
    }

    override fun setPlacedBy(
        worldIn: Level,
        pos: BlockPos,
        state: BlockState,
        placer: LivingEntity?,
        stack: ItemStack
    ) {
        super.setPlacedBy(worldIn, pos, state, placer, stack)
        if (!worldIn.isClientSide) {
            val relative = pos.relative(state.getValue(FACING))
            worldIn.setBlock(
                relative,
                state.setValue(PART, BedPart.HEAD),
                3
            )
            worldIn.blockUpdated(pos, Blocks.AIR)
            state.updateNeighbourShapes(worldIn, pos, 3)
        }
    }

    override fun playerWillDestroy(level: Level, pos: BlockPos, blockState: BlockState, player: Player) {
        if (!level.isClientSide && player.isCreative) {
            val bedPart = blockState.getValue(PART)
            if (bedPart == BedPart.FOOT) {
                val blockpos = pos.relative(oppositeDirection(bedPart, blockState.getValue(FACING) as Direction))
                val blockstate = level.getBlockState(blockpos)
                if (blockstate.`is`(this) && blockstate.getValue(PART) == BedPart.HEAD) {
                    level.setBlock(blockpos, Blocks.AIR.defaultBlockState(), 35)
                    level.levelEvent(player, 2001, blockpos, getId(blockstate))
                }
            }
        }

        super.playerWillDestroy(level, pos, blockState, player)
    }

    override fun updateShape(
        state: BlockState,
        direction: Direction,
        facingState: BlockState,
        level: LevelAccessor,
        pos: BlockPos,
        neighborPos: BlockPos
    ): BlockState {
        return if (direction != oppositeDirection(state.getValue(PART), state.getValue(FACING))) {
            super.updateShape(state, direction, facingState, level, pos, neighborPos)
        } else {
            if (facingState.`is`(this) && facingState.getValue(PART) != state.getValue(PART)) state else Blocks.AIR.defaultBlockState()
        }
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)
        val part = state.getValue(PART)
        if (part == BedPart.FOOT) return

        val hasSignal = level.getBestNeighborSignal(pos) > 0
        val currentEnabled = state.getValue(ENABLED)

        if (hasSignal != currentEnabled) {
            val newState = state.setValue(ENABLED, hasSignal)
            level.setBlock(pos, newState, 3)

            val otherPos = pos.relative(oppositeDirection(part, state.getValue(FACING)))
            val otherState = level.getBlockState(otherPos)
            if (otherState.`is`(this)) {
                val newOtherState = otherState.setValue(ENABLED, hasSignal)
                level.setBlock(otherPos, newOtherState, 3)
            }
        }
    }

    private fun checkPoweredState(level: Level, pos: BlockPos, state: BlockState, flags: Int) {
        val flag = !level.hasNeighborSignal(pos)
        if (flag != state.getValue(ENABLED)) {
            level.setBlock(pos, state.setValue(ENABLED, flag), flags)

            if (state.getValue(PART) == BedPart.HEAD) {
                val neighborPos = getRootPos(pos, state)
                val neighborState = level.getBlockState(neighborPos)
                level.setBlock(neighborPos, neighborState.setValue(ENABLED, flag), flags)
            }
        }
    }
}