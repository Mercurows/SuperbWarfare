package com.atsuishio.superbwarfare.event;

import com.atsuishio.superbwarfare.tools.PhysicsManager;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PhysicsEventHandler {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 更新物理世界
            PhysicsManager.getInstance().update();
        }
    }
}
