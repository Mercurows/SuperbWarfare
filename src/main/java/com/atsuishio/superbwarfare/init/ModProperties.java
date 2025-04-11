package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.Mod;
import com.atsuishio.superbwarfare.tools.NBTTool;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModProperties {

    @SubscribeEvent
    public static void propertyOverrideRegistry(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(ModItems.MONITOR.get(), Mod.loc("monitor_linked"),
                (itemStack, clientWorld, livingEntity, seed) -> NBTTool.getTag(itemStack).getBoolean("Linked") ? 1.0F : 0.0F));
        event.enqueueWork(() -> ItemProperties.register(ModItems.ARMOR_PLATE.get(), Mod.loc("armor_plate_infinite"),
                (itemStack, clientWorld, livingEntity, seed) -> NBTTool.getTag(itemStack).getBoolean("Infinite") ? 1.0F : 0.0F));
    }
}