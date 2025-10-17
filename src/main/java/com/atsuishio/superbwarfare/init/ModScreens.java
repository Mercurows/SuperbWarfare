package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.screens.*;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleContainerType;
import com.atsuishio.superbwarfare.menu.VehicleMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModScreens {

    @SubscribeEvent
    public static void clientLoad(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.REFORGING_TABLE_MENU.get(), ReforgingTableScreen::new);
        event.register(ModMenuTypes.CHARGING_STATION_MENU.get(), ChargingStationScreen::new);

        event.register(ModMenuTypes.VEHICLE_MENU_MINI.get(),
                (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.MINI));
        event.register(ModMenuTypes.VEHICLE_MENU_MINI_UPGRADE.get(),
                (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.MINI));

        event.register(ModMenuTypes.VEHICLE_MENU_SMALL.get(),
                (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.SMALL));
        event.register(ModMenuTypes.VEHICLE_MENU_SMALL_UPGRADE.get(),
                (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.SMALL));

        event.register(ModMenuTypes.VEHICLE_MENU_MEDIUM.get(),
                (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.MEDIUM));
        event.register(ModMenuTypes.VEHICLE_MENU_MEDIUM_UPGRADE.get(),
                (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.MEDIUM));

        event.register(ModMenuTypes.VEHICLE_MENU_LARGE.get(),
                (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.LARGE));
        event.register(ModMenuTypes.VEHICLE_MENU_LARGE_UPGRADE.get(),
                (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.LARGE));

        event.register(ModMenuTypes.VEHICLE_MENU_HUGE.get(),
                (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.HUGE));
        event.register(ModMenuTypes.VEHICLE_MENU_HUGE_UPGRADE.get(),
                (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.HUGE));

        event.register(ModMenuTypes.SUPERB_ITEM_INTERFACE_MENU.get(), SuperbItemInterfaceScreen::new);
        event.register(ModMenuTypes.FUMO_25_MENU.get(), FuMO25Screen::new);
        event.register(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), VehicleAssemblingScreen::new);
    }
}
