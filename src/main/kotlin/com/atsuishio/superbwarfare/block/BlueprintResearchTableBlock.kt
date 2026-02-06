package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.mojang.serialization.MapCodec
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
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
import net.minecraft.world.level.block.state.properties.BedPart
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.EnumProperty
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
class BlueprintResearchTableBlock : BaseEntityBlock(Properties.of().strength(2f)) {

    companion object {
        @JvmField
        val PART: EnumProperty<BedPart> = BlockStateProperties.BED_PART

        @JvmField
        val FACING: DirectionProperty = BlockStateProperties.HORIZONTAL_FACING

        fun oppositeDirection(part: BedPart, direction: Direction): Direction =
            if (part == BedPart.FOOT) direction else direction.opposite
    }

    init {
        this.registerDefaultState(
            this.stateDefinition.any().setValue(PART, BedPart.FOOT).setValue(FACING, Direction.NORTH)
        )
    }

    override fun isPathfindable(
        state: BlockState,
        pathComputationType: PathComputationType
    ) = false

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block, BlockState>) {
        pBuilder.add(PART, FACING)
    }

    override fun useWithoutItem(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS
        } else {
            this.openContainer(level, pos, state, player)
            return InteractionResult.CONSUME
        }
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: Item.TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.blueprint_research_table").withStyle(ChatFormatting.GRAY)
        )
    }

    private fun openContainer(level: Level, pos: BlockPos, state: BlockState, player: Player) {
        val entity = level.getBlockEntity(this.getRootPos(pos, state)) as? BlueprintResearchTableBlockEntity ?: return
        player.openMenu(entity)
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
        pBlockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        if (!pLevel.isClientSide) {
            return createTickerHelper(
                pBlockEntityType,
                ModBlockEntities.BLUEPRINT_RESEARCH_TABLE.get(),
                BlockEntityTicker { pLevel: Level, pPos: BlockPos, pState: BlockState, blockEntity: BlueprintResearchTableBlockEntity ->
                    BlueprintResearchTableBlockEntity.serverTick(
                        pLevel,
                        pPos,
                        pState,
                        blockEntity
                    )
                })
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
        val direction = context.horizontalDirection.clockWise
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

    override fun playerWillDestroy(level: Level, pos: BlockPos, blockState: BlockState, player: Player): BlockState {
        if (!level.isClientSide && player.isCreative) {
            val bedPart = blockState.getValue(PART)
            if (bedPart == BedPart.FOOT) {
                val targetPos = pos.relative(oppositeDirection(bedPart, blockState.getValue(FACING) as Direction))
                val targetState = level.getBlockState(targetPos)
                if (targetState.`is`(this) && targetState.getValue(PART) == BedPart.HEAD) {
                    level.setBlock(targetPos, Blocks.AIR.defaultBlockState(), 35)
                    level.levelEvent(player, 2001, targetPos, getId(targetState))
                }
            }
        }

        return super.playerWillDestroy(level, pos, blockState, player)
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

    val codec: MapCodec<BlueprintResearchTableBlock> = simpleCodec { _ -> BlueprintResearchTableBlock() }

    override fun codec(): MapCodec<out BaseEntityBlock> = codec
}