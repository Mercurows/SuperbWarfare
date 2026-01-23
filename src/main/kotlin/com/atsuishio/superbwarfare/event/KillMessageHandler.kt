package com.atsuishio.superbwarfare.event

import com.atsuishio.superbwarfare.tools.LivingKillRecord
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import java.util.*

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, value = [Dist.CLIENT])
object KillMessageHandler {
    val QUEUE: Queue<LivingKillRecord> = ArrayDeque()

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return

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
