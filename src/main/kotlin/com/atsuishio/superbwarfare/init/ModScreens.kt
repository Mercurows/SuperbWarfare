package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.client.screens.*
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleContainerType
import net.minecraft.client.gui.screens.MenuScreens

object ModScreens {
    fun register() {
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU_MINI.get()) { menu, inventory, title ->
            VehicleScreen(
                menu,
                inventory,
                title,
                VehicleContainerType.MINI
            )
        }
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU_MINI_UPGRADE.get()) { menu, inventory, title ->
            VehicleScreen(
                menu,
                inventory,
                title,
                VehicleContainerType.MINI
            )
        }
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU_SMALL.get()) { menu, inventory, title ->
            VehicleScreen(
                menu,
                inventory,
                title,
                VehicleContainerType.SMALL
            )
        }
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU_SMALL_UPGRADE.get()) { menu, inventory, title ->
            VehicleScreen(
                menu,
                inventory,
                title,
                VehicleContainerType.SMALL
            )
        }
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU_MEDIUM.get()) { menu, inventory, title ->
            VehicleScreen(
                menu,
                inventory,
                title,
                VehicleContainerType.MEDIUM
            )
        }
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU_MEDIUM_UPGRADE.get()) { menu, inventory, title ->
            VehicleScreen(
                menu,
                inventory,
                title,
                VehicleContainerType.MEDIUM
            )
        }
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU_LARGE.get()) { menu, inventory, title ->
            VehicleScreen(
                menu,
                inventory,
                title,
                VehicleContainerType.LARGE
            )
        }
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU_LARGE_UPGRADE.get()) { menu, inventory, title ->
            VehicleScreen(
                menu,
                inventory,
                title,
                VehicleContainerType.LARGE
            )
        }
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU_HUGE.get()) { menu, inventory, title ->
            VehicleScreen(
                menu,
                inventory,
                title,
                VehicleContainerType.HUGE
            )
        }
        MenuScreens.register(ModMenuTypes.VEHICLE_MENU_HUGE_UPGRADE.get()) { menu, inventory, title ->
            VehicleScreen(
                menu,
                inventory,
                title,
                VehicleContainerType.HUGE
            )
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