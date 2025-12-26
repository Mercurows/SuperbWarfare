package com.atsuishio.superbwarfare.compat;

import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CompatHolder {

    public static final String DMV = "dreamaticvoyage";
    public static final String VRC = "virtuarealcraft";
    public static final String CLOTH_CONFIG = "cloth_config";
    public static final String COLD_SWEAT = "cold_sweat";
    public static final String REALCAMERA = "realcamera";
    public static final String NET_MUSIC = "netmusic";

    @SubscribeEvent
    public static void onInterModEnqueue(final InterModEnqueueEvent event) {
        event.enqueueWork(() -> hasMod(CLOTH_CONFIG, () -> DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> ClothConfigHelper::registerScreen)));
    }

    public static void hasMod(String modid, Runnable runnable) {
        if (ModList.get().isLoaded(modid)) {
            runnable.run();
        }
    }
}
