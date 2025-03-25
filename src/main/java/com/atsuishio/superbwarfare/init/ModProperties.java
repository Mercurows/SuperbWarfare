package com.atsuishio.superbwarfare.init;

import com.atsuishio.superbwarfare.ModUtils;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ModProperties {
    @SubscribeEvent
    public static void propertyOverrideRegistry(FMLClientSetupEvent event) {
//        event.enqueueWork(() -> ItemProperties.register(ModItems.MONITOR.get(), ModUtils.loc("monitor_linked"),
//                (itemStack, clientWorld, livingEntity, seed) -> ItemNBTTool.getBoolean(itemStack, "Linked", false) ? 1.0F : 0.0F));
//        event.enqueueWork(() -> ItemProperties.register(ModItems.ARMOR_PLATE.get(), ModUtils.loc("armor_plate_infinite"),
//                (itemStack, clientWorld, livingEntity, seed) -> ItemNBTTool.getBoolean(itemStack, "Infinite", false) ? 1.0F : 0.0F));
    }
}