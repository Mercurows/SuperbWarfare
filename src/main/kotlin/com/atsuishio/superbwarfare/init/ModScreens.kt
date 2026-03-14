package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.client.screens.*
import net.minecraft.client.gui.screens.MenuScreens

object ModScreens {
    fun register() {
        MenuScreens.register(ModMenuTypes.MINI_VEHICLE_CONTAINER_MENU.get(), ::MiniVehicleContainerScreen)
        MenuScreens.register(ModMenuTypes.SMALL_VEHICLE_CONTAINER_MENU.get(), ::SmallVehicleContainerScreen)
        MenuScreens.register(ModMenuTypes.MEDIUM_VEHICLE_CONTAINER_MENU.get(), ::MediumVehicleContainerScreen)
        MenuScreens.register(ModMenuTypes.LARGE_VEHICLE_CONTAINER_MENU.get(), ::LargeVehicleContainerScreen)
        MenuScreens.register(ModMenuTypes.HUGE_VEHICLE_CONTAINER_MENU.get(), ::HugeVehicleContainerScreen)
        MenuScreens.register(ModMenuTypes.REFORGING_TABLE_MENU.get(), ::ReforgingTableScreen)
        MenuScreens.register(ModMenuTypes.CHARGING_STATION_MENU.get(), ::ChargingStationScreen)
        MenuScreens.register(ModMenuTypes.SUPERB_ITEM_INTERFACE_MENU.get(), ::SuperbItemInterfaceScreen)
        MenuScreens.register(ModMenuTypes.FUMO_25_MENU.get(), ::FuMO25Screen)
        MenuScreens.register(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), ::VehicleAssemblingScreen)
        MenuScreens.register(ModMenuTypes.BLUEPRINT_RESEARCH_TABLE.get(), ::BlueprintResearchTableScreen)
    }
}