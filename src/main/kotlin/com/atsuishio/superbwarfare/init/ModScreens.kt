package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.client.screens.*
import net.minecraft.client.gui.screens.MenuScreens

object ModScreens {
    fun register() {
        MenuScreens.register(ModMenuTypes.SMALL_VEHICLE_CONTAINER_MENU.get()) { menu, inv, title ->
            SmallVehicleContainerScreen(menu, inv, title)
        }
        MenuScreens.register(ModMenuTypes.MEDIUM_VEHICLE_CONTAINER_MENU.get()) { menu, inv, title ->
            MediumVehicleContainerScreen(menu, inv, title)
        }

        MenuScreens.register(ModMenuTypes.REFORGING_TABLE_MENU.get()) { pMenu, pPlayerInventory, pTitle ->
            ReforgingTableScreen(
                pMenu,
                pPlayerInventory,
                pTitle
            )
        }
        MenuScreens.register(ModMenuTypes.CHARGING_STATION_MENU.get()) { pMenu, pPlayerInventory, pTitle ->
            ChargingStationScreen(
                pMenu,
                pPlayerInventory,
                pTitle
            )
        }
        MenuScreens.register(ModMenuTypes.SUPERB_ITEM_INTERFACE_MENU.get()) { menu, playerInventory, title ->
            SuperbItemInterfaceScreen(
                menu,
                playerInventory,
                title
            )
        }
        MenuScreens.register(ModMenuTypes.FUMO_25_MENU.get()) { pMenu, pPlayerInventory, pTitle ->
            FuMO25Screen(
                pMenu,
                pPlayerInventory,
                pTitle
            )
        }
        MenuScreens.register(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get()) { pMenu, pPlayerInventory, pTitle ->
            VehicleAssemblingScreen(
                pMenu,
                pPlayerInventory,
                pTitle
            )
        }
        MenuScreens.register(ModMenuTypes.BLUEPRINT_RESEARCH_TABLE.get()) { menu, playerInventory, title ->
            BlueprintResearchTableScreen(
                menu,
                playerInventory,
                title
            )
        }
    }
}