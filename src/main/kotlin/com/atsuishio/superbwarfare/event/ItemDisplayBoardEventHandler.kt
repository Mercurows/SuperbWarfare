package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.block.ItemDisplayBoardBlock
import com.atsuishio.superbwarfare.block.entity.ItemDisplayBoardBlockEntity
import net.minecraft.world.InteractionResult
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

/**
 * 物品展示板的潜行右键取下物品逻辑。
 *
 * 原版在潜行且手中持有物品时会跳过方块自身的 use()（潜行放置方块用），
 * 因此这里提前拦截右键事件，保证手持任意物品时都能取下展示的物品。
 */
@Mod.EventBusSubscriber
object ItemDisplayBoardEventHandler {

    @SubscribeEvent
    fun onRightClickBlock(event: PlayerInteractEvent.RightClickBlock) {
        val level = event.level
        val pos = event.pos
        val state = level.getBlockState(pos)
        if (state.block !is ItemDisplayBoardBlock) return

        val player = event.entity
        if (player.isSpectator) return
        if (!player.isShiftKeyDown) return

        val blockEntity = level.getBlockEntity(pos) as? ItemDisplayBoardBlockEntity ?: return
        if (blockEntity.item.isEmpty) return

        event.isCanceled = true
        event.setCancellationResult(InteractionResult.SUCCESS)

        if (level.isClientSide) return

        ItemDisplayBoardBlock.takeDisplayedItem(level, pos, state, player)
    }
}
