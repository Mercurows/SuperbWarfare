package com.atsuishio.superbwarfare.compat;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent;
import net.neoforged.fml.loading.FMLEnvironment;

@EventBusSubscriber(modid = Mod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class CompatHolder {

    public static final String DMV = "dreamaticvoyage";
    public static final String VRC = "virtuarealcraft";
    public static final String CLOTH_CONFIG = "cloth_config";
    public static final String COLD_SWEAT = "cold_sweat";
    public static final String REALCAMERA = "realcamera";
    public static final String NET_MUSIC = "netmusic";

    @SubscribeEvent
    public static void onInterModEnqueue(final InterModEnqueueEvent event) {
        event.enqueueWork(() -> hasMod(CLOTH_CONFIG, () -> {
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ClothConfigHelper.registerScreen();
            }
        }));
    }

    public static void hasMod(String modid, Runnable runnable) {
        if (ModList.get().isLoaded(modid)) {
            runnable.run();
        }
    }
}
