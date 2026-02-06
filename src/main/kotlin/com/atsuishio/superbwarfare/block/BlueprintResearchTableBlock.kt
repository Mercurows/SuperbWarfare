package com.atsuishio.superbwarfare.block

import com.atsuishio.superbwarfare.block.entity.BlueprintResearchTableBlockEntity
import com.atsuishio.superbwarfare.init.ModBlockEntities
import com.mojang.serialization.MapCodec
import net.minecraft.ChatFormatting
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.Containers
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult

@Suppress("OVERRIDE_DEPRECATION")
class BlueprintResearchTableBlock : BaseEntityBlock(Properties.of().strength(2f)) {
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
            this.openContainer(level, pos, player)
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

    private fun openContainer(level: Level, pos: BlockPos, player: Player) {
        val entity = level.getBlockEntity(pos) as? BlueprintResearchTableBlockEntity ?: return
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

    val codec: MapCodec<BlueprintResearchTableBlock> = simpleCodec { _ -> BlueprintResearchTableBlock() }

    override fun codec(): MapCodec<out BaseEntityBlock> = codec
}