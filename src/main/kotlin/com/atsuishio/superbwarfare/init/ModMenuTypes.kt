package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.inventory.menu.*
import net.minecraft.world.inventory.MenuType
import net.minecraftforge.common.extensions.IForgeMenuType
import net.minecraftforge.registries.DeferredRegister
import net.minecraftforge.registries.ForgeRegistries
import net.minecraftforge.registries.RegistryObject

object ModMenuTypes {
    @JvmField
    val REGISTRY: DeferredRegister<MenuType<*>> = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Mod.MODID)

    @JvmField
    val REFORGING_TABLE_MENU: RegistryObject<MenuType<ReforgingTableMenu>> =
        REGISTRY.register("reforging_table_menu") {
            IForgeMenuType.create { windowId, inv, _ -> ReforgingTableMenu(windowId, inv) }
        }

    @JvmField
    val CHARGING_STATION_MENU: RegistryObject<MenuType<ChargingStationMenu>> =
        REGISTRY.register("charging_station_menu") {
            IForgeMenuType.create { windowId, inv, _ -> ChargingStationMenu(windowId, inv) }
        }

    @JvmField
    val MINI_VEHICLE_CONTAINER_MENU: RegistryObject<MenuType<MiniVehicleContainerMenu>> =
        REGISTRY.register("mini_vehicle_container") { MiniVehicleContainerMenu.TYPE }

    @JvmField
    val SMALL_VEHICLE_CONTAINER_MENU: RegistryObject<MenuType<SmallVehicleContainerMenu>> =
        REGISTRY.register("small_vehicle_container") { SmallVehicleContainerMenu.TYPE }

    @JvmField
    val MEDIUM_VEHICLE_CONTAINER_MENU: RegistryObject<MenuType<MediumVehicleContainerMenu>> =
        REGISTRY.register("medium_vehicle_container") { MediumVehicleContainerMenu.TYPE }

    @JvmField
    val LARGE_VEHICLE_CONTAINER_MENU: RegistryObject<MenuType<LargeVehicleContainerMenu>> =
        REGISTRY.register("large_vehicle_container") { LargeVehicleContainerMenu.TYPE }

    @JvmField
    val HUGE_VEHICLE_CONTAINER_MENU: RegistryObject<MenuType<HugeVehicleContainerMenu>> =
        REGISTRY.register("huge_vehicle_container") { HugeVehicleContainerMenu.TYPE }

    @JvmField
    val SUPERB_ITEM_INTERFACE_MENU: RegistryObject<MenuType<SuperbItemInterfaceMenu>> =
        REGISTRY.register("superb_item_interface_menu") {
            IForgeMenuType.create { windowId, inv, _ -> SuperbItemInterfaceMenu(windowId, inv) }
        }

    @JvmField
    val FUMO_25_MENU: RegistryObject<MenuType<FuMO25Menu>> =
        REGISTRY.register("fumo_25_menu") {
            IForgeMenuType.create { windowId, inv, _ -> FuMO25Menu(windowId, inv) }
        }

    @JvmField
    val VEHICLE_ASSEMBLING_MENU: RegistryObject<MenuType<VehicleAssemblingMenu>> =
        REGISTRY.register("vehicle_assembling_menu") {
            IForgeMenuType.create { windowId, inv, _ -> VehicleAssemblingMenu(windowId, inv) }
        }

    @JvmField
    val BLUEPRINT_RESEARCH_TABLE: RegistryObject<MenuType<BlueprintResearchTableMenu>> =
        REGISTRY.register("blueprint_research_table_menu") {
            IForgeMenuType.create { windowId, inv, _ -> BlueprintResearchTableMenu(windowId, inv) }
        }
}