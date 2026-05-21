package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.resource.model.*
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent

@EventBusSubscriber
object BedrockModelLoader {
    @SubscribeEvent
    fun onAddClientResourceListener(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(VehicleModelReloadListener)
        event.registerReloadListener(VehicleLODModelReloadListener)
        event.registerReloadListener(ProjectileModelReloadListener)
        event.registerReloadListener(EntityModelReloadListener)
        event.registerReloadListener(ArmorModelReloadListener)
        event.registerReloadListener(BlockModelReloadListener)
    }
}