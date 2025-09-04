package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.tools.LivingKillRecord;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Queue;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class KillMessageHandler {

    public static Queue<LivingKillRecord> QUEUE = new ArrayDeque<>();

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        for (LivingKillRecord record : QUEUE) {
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
