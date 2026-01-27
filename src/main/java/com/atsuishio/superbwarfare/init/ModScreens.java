package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.client.screens.*;
import com.atsuishio.superbwarfare.data.vehicle.subdata.VehicleContainerType;
import com.atsuishio.superbwarfare.menu.VehicleMenu;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModScreens {

    @SubscribeEvent
    public static void clientLoad(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenuTypes.REFORGING_TABLE_MENU.get(), ReforgingTableScreen::new);
            MenuScreens.register(ModMenuTypes.CHARGING_STATION_MENU.get(), ChargingStationScreen::new);

            MenuScreens.register(ModMenuTypes.VEHICLE_MENU_MINI.get(),
                    (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.MINI));
            MenuScreens.register(ModMenuTypes.VEHICLE_MENU_MINI_UPGRADE.get(),
                    (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.MINI));

            MenuScreens.register(ModMenuTypes.VEHICLE_MENU_SMALL.get(),
                    (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.SMALL));
            MenuScreens.register(ModMenuTypes.VEHICLE_MENU_SMALL_UPGRADE.get(),
                    (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.SMALL));

            MenuScreens.register(ModMenuTypes.VEHICLE_MENU_MEDIUM.get(),
                    (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.MEDIUM));
            MenuScreens.register(ModMenuTypes.VEHICLE_MENU_MEDIUM_UPGRADE.get(),
                    (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.MEDIUM));

            MenuScreens.register(ModMenuTypes.VEHICLE_MENU_LARGE.get(),
                    (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.LARGE));
            MenuScreens.register(ModMenuTypes.VEHICLE_MENU_LARGE_UPGRADE.get(),
                    (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.LARGE));

            MenuScreens.register(ModMenuTypes.VEHICLE_MENU_HUGE.get(),
                    (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.HUGE));
            MenuScreens.register(ModMenuTypes.VEHICLE_MENU_HUGE_UPGRADE.get(),
                    (VehicleMenu menu, Inventory inventory, Component title) -> new VehicleScreen(menu, inventory, title, VehicleContainerType.HUGE));

            MenuScreens.register(ModMenuTypes.SUPERB_ITEM_INTERFACE_MENU.get(), SuperbItemInterfaceScreen::new);
            MenuScreens.register(ModMenuTypes.FUMO_25_MENU.get(), FuMO25Screen::new);
            MenuScreens.register(ModMenuTypes.VEHICLE_ASSEMBLING_MENU.get(), VehicleAssemblingScreen::new);
            MenuScreens.register(ModMenuTypes.BLUEPRINT_RESEARCH_TABLE.get(), BlueprintResearchTableScreen::new);
        });
    }
}
