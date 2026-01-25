package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.tools.NBTTool
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraft.world.item.ItemStack
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ModProperties {
    @SubscribeEvent
    fun propertyOverrideRegistry(event: FMLClientSetupEvent) {
        event.enqueueWork {
            ItemProperties.register(ModItems.MONITOR.get(), loc("monitor_linked")) { itemStack: ItemStack, _, _, _ ->
                if (NBTTool.getTag(itemStack).getBoolean("Linked")) 1f else 0f
            }
        }
        event.enqueueWork {
            ItemProperties.register(
                ModItems.ARMOR_PLATE.get(),
                loc("armor_plate_infinite")
            ) { itemStack: ItemStack, _, _, _ ->
                if (NBTTool.getTag(itemStack).getBoolean("Infinite")) 1f else 0f
            }
        }

    }
}