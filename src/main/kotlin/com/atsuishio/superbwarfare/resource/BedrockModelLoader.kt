package com.atsuishio.superbwarfare.resource

import com.atsuishio.superbwarfare.resource.model.*
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(
    bus = Mod.EventBusSubscriber.Bus.MOD,
    modid = com.atsuishio.superbwarfare.Mod.MODID,
    value = [Dist.CLIENT]
)
object BedrockModelLoader {
    @SubscribeEvent
    fun onAddClientResourceListener(event: RegisterClientReloadListenersEvent) {
        event.registerReloadListener(VehicleModelReloadListener)
        event.registerReloadListener(VehicleLODModelReloadListener)
        event.registerReloadListener(ProjectileModelReloadListener)
        event.registerReloadListener(EntityModelReloadListener)
        event.registerReloadListener(ArmorModelReloadListener)
        event.registerReloadListener(BlockModelReloadListener)
        event.registerReloadListener(ItemModelReloadListener)
    }
}