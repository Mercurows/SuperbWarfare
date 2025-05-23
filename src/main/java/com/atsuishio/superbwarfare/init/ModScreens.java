package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.client.screens.*;
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
        event.register(ModMenuTypes.VEHICLE_MENU.get(), VehicleScreen::new);
        event.register(ModMenuTypes.FUMO_25_MENU.get(), FuMO25Screen::new);
        event.register(ModMenuTypes.DOG_TAG_EDITOR_MENU.get(), DogTagEditorScreen::new);
    }
}
