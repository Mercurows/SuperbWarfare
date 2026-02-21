package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.inventory.menu.BlueprintResearchTableMenu
import com.atsuishio.superbwarfare.inventory.menu.MediumVehicleContainerMenu
import com.atsuishio.superbwarfare.inventory.menu.SmallVehicleContainerMenu
import com.atsuishio.superbwarfare.menu.*
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.MenuType
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension
import net.neoforged.neoforge.network.IContainerFactory
import net.neoforged.neoforge.registries.DeferredHolder
import net.neoforged.neoforge.registries.DeferredRegister
import java.util.function.Supplier

object ModMenuTypes {
    @JvmField
    val REGISTRY: DeferredRegister<MenuType<*>> = DeferredRegister.create(BuiltInRegistries.MENU, Mod.MODID)

    private fun <T : AbstractContainerMenu> register(
        name: String, factory: IContainerFactory<T>
    ): DeferredHolder<MenuType<*>, MenuType<T>> =
        REGISTRY.register(name, Supplier { IMenuTypeExtension.create(factory) })

    private fun <T : AbstractContainerMenu> register(
        name: String, factory: () -> MenuType<T>
    ): DeferredHolder<MenuType<*>, MenuType<T>> =
        REGISTRY.register(name, factory)

    @JvmField
    val REFORGING_TABLE_MENU =
        register("reforging_table_menu") { windowId, inv, _ -> ReforgingTableMenu(windowId, inv) }

    @JvmField
    val CHARGING_STATION_MENU =
        register("charging_station_menu") { windowId, inv, _ -> ChargingStationMenu(windowId, inv) }

    @JvmField
    val SMALL_VEHICLE_CONTAINER_MENU = register("small_vehicle_container") { SmallVehicleContainerMenu.TYPE }

    @JvmField
    val MEDIUM_VEHICLE_CONTAINER_MENU = register("medium_vehicle_container") { MediumVehicleContainerMenu.TYPE }

    @JvmField
    val SUPERB_ITEM_INTERFACE_MENU =
        register("superb_item_interface_menu") { windowId, inv, _ -> SuperbItemInterfaceMenu(windowId, inv) }

    @JvmField
    val FUMO_25_MENU = register("fumo_25_menu") { windowId, inv, _ -> FuMO25Menu(windowId, inv) }

    @JvmField
    val VEHICLE_ASSEMBLING_MENU =
        register("vehicle_assembling_menu") { windowId, inv, _ -> VehicleAssemblingMenu(windowId, inv) }

    @JvmField
    val BLUEPRINT_RESEARCH_TABLE =
        register("blueprint_research_table_menu") { windowId, inv, _ -> BlueprintResearchTableMenu(windowId, inv) }
}
