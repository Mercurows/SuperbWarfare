package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.tools.HitboxHelper
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.loading.FMLEnvironment
import net.neoforged.neoforge.event.entity.player.PlayerEvent
import net.neoforged.neoforge.event.tick.PlayerTickEvent

@EventBusSubscriber
object HitboxHelperEventHandler {
    @SubscribeEvent(receiveCanceled = true)
    fun onPlayerTick(event: PlayerTickEvent.Post) {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            HitboxHelper.onPlayerTick(event.entity)
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onPlayerLoggedOut(event: PlayerEvent.PlayerLoggedOutEvent) {
        HitboxHelper.onPlayerLoggedOut(event.entity)
    }
}