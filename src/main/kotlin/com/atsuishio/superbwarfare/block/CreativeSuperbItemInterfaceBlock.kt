package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.CreativeSuperbItemInterfaceBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.atsuishio.superbwarfare.init.ModTags
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.Containers
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class CreativeSuperbItemInterfaceBlock : SuperbItemInterfaceBlock() {
    override fun appendHoverText(
        pStack: ItemStack,
        pLevel: BlockGetter?,
        pTooltip: MutableList<Component?>,
        pFlag: TooltipFlag
    ) {
        pTooltip.add(
            Component.translatable("des.superbwarfare.creative_superb_item_interface").withStyle(ChatFormatting.GRAY)
        )
    }

    override fun newBlockEntity(pos: BlockPos, state: BlockState): BlockEntity {
        return CreativeSuperbItemInterfaceBlockEntity(pos, state)
    }

    override fun <T : BlockEntity?> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T?>
    ): BlockEntityTicker<T?>? {
        return if (pLevel.isClientSide) null else createTickerHelper<CreativeSuperbItemInterfaceBlockEntity, T?>(
            pBlockEntityType,
            ModBlockEntities.CREATIVE_SUPERB_ITEM_INTERFACE.get()
        ) { level, pos, state, blockEntity ->
            CreativeSuperbItemInterfaceBlockEntity.serverTick(
                level,
                pos,
                state,
                blockEntity
            )
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
        val stack = pPlayer.getItemInHand(pHand)
        if (stack.`is`(ModTags.Items.TOOLS_CROWBAR) || stack.`is`(ModTags.Items.WRENCHES) || stack.`is`(ModTags.Items.TOOLS_WRENCH)) {
            var facing = pHit.direction
            if (state.getValue(FACING) == facing) {
                facing = facing.opposite
            }
            pLevel.setBlockAndUpdate(pPos, state.setValue(FACING, facing))
            return InteractionResult.SUCCESS
        }

        if (pLevel.isClientSide) {
            return InteractionResult.SUCCESS
        } else {
            val blockEntity = pLevel.getBlockEntity(pPos)
            if (blockEntity is CreativeSuperbItemInterfaceBlockEntity) {
                pPlayer.openMenu(blockEntity)
            }

            return InteractionResult.CONSUME
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
            if (blockEntity is CreativeSuperbItemInterfaceBlockEntity) {
                Containers.dropContents(pLevel, pPos, blockEntity)
                pLevel.updateNeighbourForOutputSignal(pPos, this)
            }

            super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving)
        }
    }
}
