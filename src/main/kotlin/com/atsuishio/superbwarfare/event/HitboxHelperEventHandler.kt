package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.tools.HitboxHelper
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.entity.player.PlayerEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.LogicalSide
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber
object HitboxHelperEventHandler {
    @SubscribeEvent(receiveCanceled = true)
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.side == LogicalSide.SERVER && event.phase == TickEvent.Phase.END) {
            HitboxHelper.onPlayerTick(event.player)
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onPlayerLoggedOut(event: PlayerEvent.PlayerLoggedOutEvent) {
        HitboxHelper.onPlayerLoggedOut(event.entity)
    }
}