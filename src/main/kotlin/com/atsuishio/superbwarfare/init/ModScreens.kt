package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.client.screens.*
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent

@EventBusSubscriber(modid = Mod.MODID, value = [Dist.CLIENT])
object ModScreens {
    @SubscribeEvent
    fun clientLoad(event: RegisterMenuScreensEvent) {
        event.register(ModMenuTypes.MINI_VEHICLE_CONTAINER_MENU.get(), ::MiniVehicleContainerScreen)
        event.register(ModMenuTypes.SMALL_VEHICLE_CONTAINER_MENU.get(), ::SmallVehicleContainerScreen)
        event.register(ModMenuTypes.MEDIUM_VEHICLE_CONTAINER_MENU.get(), ::MediumVehicleContainerScreen)
        event.register(ModMenuTypes.LARGE_VEHICLE_CONTAINER_MENU.get(), ::LargeVehicleContainerScreen)
        event.register(ModMenuTypes.HUGE_VEHICLE_CONTAINER_MENU.get(), ::HugeVehicleContainerScreen)

        event.register(ModMenuTypes.REFORGING_TABLE_MENU.get(), ::ReforgingTableScreen)
        event.register(ModMenuTypes.CHARGING_STATION_MENU.get(), ::ChargingStationScreen)
        event.register(ModMenuTypes.SUPERB_ITEM_INTERFACE_MENU.get(), ::SuperbItemInterfaceScreen)
        event.register(ModMenuTypes.FUMO_25_MENU.get(), ::FuMO25Screen)
        event.register(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), ::VehicleAssemblingScreen)
        event.register(ModMenuTypes.BLUEPRINT_RESEARCH_TABLE.get(), ::BlueprintResearchTableScreen)
    }
}