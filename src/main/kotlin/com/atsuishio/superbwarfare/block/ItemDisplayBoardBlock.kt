package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.ItemDisplayBoardBlockEntity
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.LevelReader
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.gameevent.GameEvent
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape

/**
 * 方块版本的物品展示框。可以附着在任意方块的 6 个面上，右键放入/旋转物品，潜行+右键取下物品。
 */
@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class ItemDisplayBoardBlock : BaseEntityBlock(
    Properties.of()
        .mapColor(MapColor.METAL)
        .sound(SoundType.METAL)
        .strength(0.8f, 6.0f)
        .noOcclusion()
) {

    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ROTATION, 0)
        )
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block?, BlockState?>) {
        pBuilder.add(FACING).add(ROTATION)
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        if (!context.canPlace()) return null

        val facing = context.clickedFace
        val pos = context.clickedPos
        val supportPos = pos.relative(facing.opposite)

        if (!context.level.getBlockState(supportPos).isFaceSturdy(context.level, supportPos, facing)) {
            return null
        }

        return this.defaultBlockState()
            .setValue(FACING, facing)
            .setValue(ROTATION, 0)
    }

    override fun canSurvive(pState: BlockState, pLevel: LevelReader, pPos: BlockPos): Boolean {
        val facing = pState.getValue(FACING)
        val supportPos = pPos.relative(facing.opposite)
        return pLevel.getBlockState(supportPos).isFaceSturdy(pLevel, supportPos, facing)
    }

    override fun updateShape(
        pState: BlockState,
        pDirection: Direction,
        pNeighborState: BlockState,
        pLevel: LevelAccessor,
        pPos: BlockPos,
        pNeighborPos: BlockPos
    ): BlockState {
        return if (pDirection == pState.getValue(FACING).opposite && !pState.canSurvive(pLevel, pPos)) {
            Blocks.AIR.defaultBlockState()
        } else {
            super.updateShape(pState, pDirection, pNeighborState, pLevel, pPos, pNeighborPos)
        }
    }

    override fun use(
        state: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pPlayer: Player,
        pHand: InteractionHand,
        pHit: BlockHitResult
    ): InteractionResult {
        if (pLevel.isClientSide) {
            val blockEntity = pLevel.getBlockEntity(pPos) as? ItemDisplayBoardBlockEntity
            val hasDisplayedItem = blockEntity != null && !blockEntity.item.isEmpty
            return if (hasDisplayedItem || !pPlayer.getItemInHand(pHand).isEmpty) {
                InteractionResult.SUCCESS
            } else {
                InteractionResult.PASS
            }
        }

        val blockEntity = pLevel.getBlockEntity(pPos) as? ItemDisplayBoardBlockEntity ?: return InteractionResult.PASS
        val heldItem = pPlayer.getItemInHand(pHand)

        // 潜行 + 右键取下物品（手持物品时由 ItemDisplayBoardEventHandler 提前拦截）
        if (pPlayer.isShiftKeyDown) {
            if (blockEntity.item.isEmpty) return InteractionResult.PASS

            takeDisplayedItem(pLevel, pPos, state, pPlayer)
            return InteractionResult.SUCCESS
        }

        // 已有物品时右键旋转物品
        if (!blockEntity.item.isEmpty) {
            pLevel.setBlock(pPos, state.cycle(ROTATION), Block.UPDATE_ALL)
            pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_ROTATE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f)
            pLevel.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, pPos)
            return InteractionResult.SUCCESS
        }

        // 空手右键旋转（展示框为空时无操作）
        if (heldItem.isEmpty) {
            return InteractionResult.PASS
        }

        // 右键放入物品
        blockEntity.setItem(heldItem)
        if (!pPlayer.abilities.instabuild) {
            heldItem.shrink(1)
        }
        pLevel.setBlock(pPos, state.setValue(ROTATION, 0), Block.UPDATE_ALL)
        pLevel.playSound(null, pPos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f)
        pLevel.gameEvent(pPlayer, GameEvent.BLOCK_CHANGE, pPos)
        return InteractionResult.SUCCESS
    }

    override fun onRemove(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pNewState: BlockState,
        pIsMoving: Boolean
    ) {
        if (!pState.`is`(pNewState.block)) {
            val blockEntity = pLevel.getBlockEntity(pPos) as? ItemDisplayBoardBlockEntity
            if (blockEntity != null) {
                val displayedItem = blockEntity.item
                if (!displayedItem.isEmpty) {
                    blockEntity.clearItem(sync = false)
                    if (!pLevel.isClientSide) {
                        popResource(pLevel, pPos, displayedItem)
                    }
                }
            }
        }
        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving)
    }

    override fun getShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape {
        return when (pState.getValue(FACING)) {
            Direction.DOWN -> SHAPE_DOWN
            Direction.UP -> SHAPE_UP
            Direction.NORTH -> SHAPE_NORTH
            Direction.SOUTH -> SHAPE_SOUTH
            Direction.WEST -> SHAPE_WEST
            Direction.EAST -> SHAPE_EAST
        }
    }

    override fun getCollisionShape(
        pState: BlockState,
        pLevel: BlockGetter,
        pPos: BlockPos,
        pContext: CollisionContext
    ): VoxelShape {
        return Shapes.empty()
    }

    override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity {
        return ItemDisplayBoardBlockEntity(pPos, pState)
    }

    override fun rotate(pState: BlockState, pRotation: Rotation): BlockState {
        return pState.setValue(FACING, pRotation.rotate(pState.getValue(FACING)))
    }

    override fun mirror(pState: BlockState, pMirror: Mirror): BlockState {
        return pState.setValue(FACING, pMirror.mirror(pState.getValue(FACING)))
    }

    override fun getCloneItemStack(pLevel: BlockGetter, pPos: BlockPos, pState: BlockState): ItemStack {
        val blockEntity = pLevel.getBlockEntity(pPos) as? ItemDisplayBoardBlockEntity
        val displayedItem = blockEntity?.item
        return if (displayedItem != null && !displayedItem.isEmpty) displayedItem.copy() else super.getCloneItemStack(pLevel, pPos, pState)
    }

    override fun hasAnalogOutputSignal(pState: BlockState): Boolean {
        return true
    }

    override fun getAnalogOutputSignal(pState: BlockState, pLevel: Level, pPos: BlockPos): Int {
        val blockEntity = pLevel.getBlockEntity(pPos) as? ItemDisplayBoardBlockEntity ?: return 0
        return if (blockEntity.item.isEmpty) 0 else pState.getValue(ROTATION) % 8 + 1
    }

    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: BlockGetter?,
        pTooltip: MutableList<Component?>,
        pFlag: TooltipFlag
    ) {
        pTooltip.add(Component.translatable("des.superbwarfare.item_display_board_1").withStyle(ChatFormatting.GRAY))
        pTooltip.add(Component.translatable("des.superbwarfare.item_display_board_2").withStyle(ChatFormatting.GRAY))
    }

    companion object {
        @JvmField
        val FACING: DirectionProperty = BlockStateProperties.FACING

        @JvmField
        val ROTATION: IntegerProperty = IntegerProperty.create("rotation", 0, 7)

        /**
         * 取下展示板上的物品并交给玩家。
         */
        @JvmStatic
        fun takeDisplayedItem(level: Level, pos: BlockPos, state: BlockState, player: Player) {
            val blockEntity = level.getBlockEntity(pos) as? ItemDisplayBoardBlockEntity ?: return
            val displayedItem = blockEntity.item
            if (displayedItem.isEmpty) return

            blockEntity.clearItem()
            level.setBlock(pos, state.setValue(ROTATION, 0), Block.UPDATE_ALL)

            if (!player.abilities.instabuild) {
                val itemToGive = displayedItem.copy()
                if (!player.inventory.add(itemToGive)) {
                    player.drop(itemToGive, false)
                }
            }

            level.playSound(null, pos, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 1.0f, 1.0f)
            level.gameEvent(player, GameEvent.BLOCK_CHANGE, pos)
        }

        private const val PIXEL = 1.0 / 16.0

        @JvmField
        val SHAPE_NORTH: VoxelShape = Shapes.box(0.0, 0.0, 15 * PIXEL, 1.0, 1.0, 1.0)

        @JvmField
        val SHAPE_SOUTH: VoxelShape = Shapes.box(0.0, 0.0, 0.0, 1.0, 1.0, PIXEL)

        @JvmField
        val SHAPE_WEST: VoxelShape = Shapes.box(0.0, 0.0, 0.0, PIXEL, 1.0, 1.0)

        @JvmField
        val SHAPE_EAST: VoxelShape = Shapes.box(15 * PIXEL, 0.0, 0.0, 1.0, 1.0, 1.0)

        @JvmField
        val SHAPE_DOWN: VoxelShape = Shapes.box(0.0, 15 * PIXEL, 0.0, 1.0, 1.0, 1.0)

        @JvmField
        val SHAPE_UP: VoxelShape = Shapes.box(0.0, 0.0, 0.0, 1.0, PIXEL, 1.0)
    }
}
