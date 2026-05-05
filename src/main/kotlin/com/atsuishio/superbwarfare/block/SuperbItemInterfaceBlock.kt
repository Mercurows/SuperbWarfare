package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.SuperbItemInterfaceBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModTags
import com.mojang.serialization.MapCodec
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.chat.Component
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.ItemInteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.DirectionProperty
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
open class SuperbItemInterfaceBlock : BaseEntityBlock(
    Properties.of().mapColor(MapColor.STONE).requiresCorrectToolForDrops()
        .strength(3f, 4.8f).sound(SoundType.METAL)
) {
    init {
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(ENABLED, true)
                .setValue(FACING, Direction.DOWN)
        )
    }

    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity {
        return SuperbItemInterfaceBlockEntity(ModBlockEntities.SUPERB_ITEM_INTERFACE.get(), pPos, pState)
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
            val blockEntity = level.getBlockEntity(pos)
            if (blockEntity is SuperbItemInterfaceBlockEntity) {
                player.openMenu(blockEntity)
            }

            return InteractionResult.CONSUME
        }
    }

    override fun <T : BlockEntity> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (pLevel.isClientSide) null else createTickerHelper<SuperbItemInterfaceBlockEntity, T>(
            pBlockEntityType,
            ModBlockEntities.SUPERB_ITEM_INTERFACE.get(),
            SuperbItemInterfaceBlockEntity::serverTick
        )
    }

    override fun getStateForPlacement(context: BlockPlaceContext): BlockState? {
        val direction = context.clickedFace.opposite
        return this.defaultBlockState()
            .setValue(FACING, direction)
            .setValue(ENABLED, true)
    }

    override fun onPlace(pState: BlockState, pLevel: Level, pPos: BlockPos, pOldState: BlockState, pIsMoving: Boolean) {
        if (!pOldState.`is`(pState.block)) {
            this.checkPoweredState(pLevel, pPos, pState, 2)
        }
    }

    override fun useItemOn(
        stack: ItemStack,
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        hitResult: BlockHitResult
    ): ItemInteractionResult {
        if (stack.`is`(ModTags.Items.TOOLS_CROWBAR) || stack.`is`(ModTags.Items.WRENCHES) || stack.`is`(ModTags.Items.TOOLS_WRENCH)) {
            var facing = hitResult.direction
            if (state.getValue(FACING) == facing) {
                facing = facing.opposite
            }
            level.setBlockAndUpdate(pos, state.setValue(FACING, facing))
            return ItemInteractionResult.SUCCESS
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult)
    }

    override fun neighborChanged(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pBlock: Block,
        pFromPos: BlockPos,
        pIsMoving: Boolean
    ) {
        this.checkPoweredState(pLevel, pPos, pState, 4)
    }

    private fun checkPoweredState(pLevel: Level, pPos: BlockPos, pState: BlockState, pFlags: Int) {
        val flag = !pLevel.hasNeighborSignal(pPos)
        if (flag != pState.getValue(ENABLED)) {
            pLevel.setBlock(pPos, pState.setValue(ENABLED, flag), pFlags)
        }
    }

    override fun onRemove(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pNewState: BlockState,
        pIsMoving: Boolean
    ) {
        if (!pState.`is`(pNewState.block)) {
            val blockEntity = pLevel.getBlockEntity(pPos)
            if (blockEntity is SuperbItemInterfaceBlockEntity) {
                Containers.dropContents(pLevel, pPos, blockEntity)
                pLevel.updateNeighbourForOutputSignal(pPos, this)
            }

            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving)
        }
    }

    override fun getRenderShape(pState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun hasAnalogOutputSignal(pState: BlockState): Boolean {
        return true
    }

    override fun getAnalogOutputSignal(pBlockState: BlockState, pLevel: Level, pPos: BlockPos): Int {
        return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(pLevel.getBlockEntity(pPos))
    }

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block?, BlockState?>) {
        pBuilder.add(ENABLED).add(FACING)
    }

    override fun isPathfindable(state: BlockState, pathComputationType: PathComputationType): Boolean {
        return false
    }

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.superb_item_interface").withStyle(ChatFormatting.GRAY)
        )
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC

    companion object {
        @JvmField
        val ENABLED: BooleanProperty = BlockStateProperties.ENABLED

        @JvmField
        val FACING: DirectionProperty = BlockStateProperties.FACING

        @JvmField
        val CODEC: MapCodec<SuperbItemInterfaceBlock> = simpleCodec { _ -> SuperbItemInterfaceBlock() }
    }
}
