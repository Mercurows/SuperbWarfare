package com.atsuishio.superbwarfare.compat;

import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.registries.ObjectHolder;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class CompatHolder {

    public static final String DMV = "dreamaticvoyage";
    public static final String VRC = "virtuarealcraft";
    public static final String CLOTH_CONFIG = "cloth_config";
    public static final String COLD_SWEAT = "cold_sweat";
    public static final String REALCAMERA = "realcamera";

    @ObjectHolder(registryName = "minecraft:mob_effect", value = VRC + ":curse_flame")
    public static final MobEffect VRC_CURSE_FLAME = null;

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
