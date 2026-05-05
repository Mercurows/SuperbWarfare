package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.CreativeSuperbItemInterfaceBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.mojang.serialization.MapCodec
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item.TooltipContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

class CreativeSuperbItemInterfaceBlock : SuperbItemInterfaceBlock() {

    override fun appendHoverText(
        stack: ItemStack,
        context: TooltipContext,
        tooltipComponents: MutableList<Component>,
        tooltipFlag: TooltipFlag
    ) {
        tooltipComponents.add(
            Component.translatable("des.superbwarfare.creative_superb_item_interface").withStyle(ChatFormatting.GRAY)
        )
    }

    override fun newBlockEntity(pPos: BlockPos, pState: BlockState): BlockEntity {
        return CreativeSuperbItemInterfaceBlockEntity(pPos, pState)
    }

    override fun <T : BlockEntity> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? {
        return if (pLevel.isClientSide) null else createTickerHelper(
            pBlockEntityType,
            ModBlockEntities.CREATIVE_SUPERB_ITEM_INTERFACE.get(),
            CreativeSuperbItemInterfaceBlockEntity::serverTick
        )
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
            if (blockEntity is CreativeSuperbItemInterfaceBlockEntity) {
                player.openMenu(blockEntity)
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

    companion object {
        @JvmStatic
        val CODEC: MapCodec<CreativeSuperbItemInterfaceBlock> = simpleCodec { _ -> CreativeSuperbItemInterfaceBlock() }
    }

    override fun codec(): MapCodec<out BaseEntityBlock> = CODEC
}
