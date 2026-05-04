package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.tools.LivingKillRecord
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.ClientTickEvent
import java.util.*

@EventBusSubscriber(Dist.CLIENT)
object KillMessageHandler {
    val QUEUE: Queue<LivingKillRecord> = ArrayDeque()

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent.Post) {
        for (record in QUEUE) {
            if (record.freeze && record.tick >= 3) {
                continue
            }
            record.tick++
            if (record.fastRemove && record.tick >= 82 || record.tick >= 100) {
                QUEUE.poll()
            }
        }
    }
}
