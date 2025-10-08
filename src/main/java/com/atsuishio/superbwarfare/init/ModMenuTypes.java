package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.menu.*;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Mod.MODID);

    public static final RegistryObject<MenuType<ReforgingTableMenu>> REFORGING_TABLE_MENU =
            REGISTRY.register("reforging_table_menu",
                    () -> IForgeMenuType.create((windowId, inv, data) -> new ReforgingTableMenu(windowId, inv)));
    public static final RegistryObject<MenuType<ChargingStationMenu>> CHARGING_STATION_MENU =
            REGISTRY.register("charging_station_menu",
                    () -> IForgeMenuType.create((windowId, inv, data) -> new ChargingStationMenu(windowId, inv)));

    public static final RegistryObject<MenuType<VehicleMenu>> VEHICLE_MENU_MINI =
            REGISTRY.register("vehicle_menu_mini",
                    () -> IForgeMenuType.create((windowId, inv, data) -> VehicleMenu.mini(windowId, inv)));
    public static final RegistryObject<MenuType<VehicleMenu>> VEHICLE_MENU_SMALL =
            REGISTRY.register("vehicle_menu_small",
                    () -> IForgeMenuType.create((windowId, inv, data) -> VehicleMenu.small(windowId, inv)));
    public static final RegistryObject<MenuType<VehicleMenu>> VEHICLE_MENU_MEDIUM =
            REGISTRY.register("vehicle_menu_medium",
                    () -> IForgeMenuType.create((windowId, inv, data) -> VehicleMenu.medium(windowId, inv)));
    public static final RegistryObject<MenuType<VehicleMenu>> VEHICLE_MENU_LARGE =
            REGISTRY.register("vehicle_menu_large",
                    () -> IForgeMenuType.create((windowId, inv, data) -> VehicleMenu.large(windowId, inv)));
    public static final RegistryObject<MenuType<VehicleMenu>> VEHICLE_MENU_HUGE =
            REGISTRY.register("vehicle_menu_huge",
                    () -> IForgeMenuType.create((windowId, inv, data) -> VehicleMenu.huge(windowId, inv)));

    public static final RegistryObject<MenuType<SuperbItemInterfaceMenu>> SUPERB_ITEM_INTERFACE_MENU =
            REGISTRY.register("superb_item_interface_menu",
                    () -> IForgeMenuType.create((windowId, inv, data) -> new SuperbItemInterfaceMenu(windowId, inv)));
    public static final RegistryObject<MenuType<FuMO25Menu>> FUMO_25_MENU =
            REGISTRY.register("fumo_25_menu",
                    () -> IForgeMenuType.create((windowId, inv, data) -> new FuMO25Menu(windowId, inv)));
    public static final RegistryObject<MenuType<VehicleAssemblingMenu>> VEHICLE_ASSEMBLING_MENU =
            REGISTRY.register("vehicle_assembling_menu",
                    () -> IForgeMenuType.create((windowId, inv, data) -> new VehicleAssemblingMenu(windowId, inv)));
}
