package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.tools.HitboxHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber
public class HitboxHelperEvent {
    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            HitboxHelper.onPlayerTick(event.getEntity());
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        HitboxHelper.onPlayerLoggedOut(event.getEntity());
    }
}
