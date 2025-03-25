package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.menu.ChargingStationMenu;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.menu.ReforgingTableMenu;
import com.atsuishio.superbwarfare.menu.VehicleMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.MENU, ModUtils.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> REFORGING_TABLE_MENU =
            REGISTRY.register("reforging_table_menu",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> new ReforgingTableMenu(windowId, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> CHARGING_STATION_MENU =
            REGISTRY.register("charging_station_menu",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> new ChargingStationMenu(windowId, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> VEHICLE_MENU =
            REGISTRY.register("vehicle_menu",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> new VehicleMenu(windowId, inv)));
    public static final DeferredHolder<MenuType<?>, MenuType<AbstractContainerMenu>> FUMO_25_MENU =
            REGISTRY.register("fumo_25_menu",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> new FuMO25Menu(windowId, inv)));
}
