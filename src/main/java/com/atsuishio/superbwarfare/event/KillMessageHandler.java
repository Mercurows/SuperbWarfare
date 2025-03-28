package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.tools.PlayerKillRecord;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayDeque;
import java.util.Queue;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class KillMessageHandler {

    public static Queue<PlayerKillRecord> QUEUE = new ArrayDeque<>();

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        for (PlayerKillRecord record : QUEUE) {
            if (record.freeze && record.tick >= 3) {
                continue;
            }
            record.tick++;
            if (record.fastRemove && record.tick >= 82 || record.tick >= 100) {
                QUEUE.poll();
            }
        }
    }
}
