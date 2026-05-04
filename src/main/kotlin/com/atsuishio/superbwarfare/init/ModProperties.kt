package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod.Companion.loc
import com.atsuishio.superbwarfare.tools.tag
import net.minecraft.client.renderer.item.ItemProperties
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent

@EventBusSubscriber(Dist.CLIENT)
object ModProperties {
    @SubscribeEvent
    fun propertyOverrideRegistry(event: FMLClientSetupEvent) {
        event.enqueueWork {
            ItemProperties.register(ModItems.MONITOR.get(), loc("monitor_linked")) { itemStack, _, _, _ ->
                if (itemStack.tag?.getBoolean("Linked") == true) 1f else 0f
            }
        }
        event.enqueueWork {
            ItemProperties.register(ModItems.ARMOR_PLATE.get(), loc("armor_plate_infinite")) { itemStack, _, _, _ ->
                if (itemStack.tag?.getBoolean("Infinite") == true) 1f else 0f
            }
        }

    }
}