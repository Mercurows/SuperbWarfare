package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.block.ItemDisplayBoardBlock
import com.atsuishio.superbwarfare.block.entity.ItemDisplayBoardBlockEntity
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.common.util.TriState
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent

/**
 * 物品展示板的潜行右键取下物品逻辑。
 *
 * 强制放行潜行时的方块交互，并阻止手持物品抢先执行 onItemUseFirst。
 * 取下操作由方块自身完成，空手与持物共用同一条客户端预测/服务端执行路径。
 */
@EventBusSubscriber
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
        if (!blockEntity.hasItem) return

        event.useBlock = TriState.TRUE
        event.useItem = TriState.FALSE
    }
}
