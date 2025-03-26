package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.ModUtils;
import com.atsuishio.superbwarfare.menu.ChargingStationMenu;
import com.atsuishio.superbwarfare.menu.FuMO25Menu;
import com.atsuishio.superbwarfare.menu.ReforgingTableMenu;
import com.atsuishio.superbwarfare.menu.VehicleMenu;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> REGISTRY = DeferredRegister.create(BuiltInRegistries.MENU, ModUtils.MODID);

    public static final Supplier<MenuType<ReforgingTableMenu>> REFORGING_TABLE_MENU =
            REGISTRY.register("reforging_table_menu",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> new ReforgingTableMenu(windowId, inv)));
    public static final Supplier<MenuType<ChargingStationMenu>> CHARGING_STATION_MENU =
            REGISTRY.register("charging_station_menu",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> new ChargingStationMenu(windowId, inv)));
    public static final Supplier<MenuType<VehicleMenu>> VEHICLE_MENU =
            REGISTRY.register("vehicle_menu",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> new VehicleMenu(windowId, inv)));
    public static final Supplier<MenuType<FuMO25Menu>> FUMO_25_MENU =
            REGISTRY.register("fumo_25_menu",
                    () -> IMenuTypeExtension.create((windowId, inv, data) -> new FuMO25Menu(windowId, inv)));
}
