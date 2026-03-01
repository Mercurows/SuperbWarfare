package com.atsuishio.superbwarfare.init

import com.atsuishio.superbwarfare.Mod.Companion.loc
import net.minecraft.client.renderer.item.ItemProperties
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = [Dist.CLIENT])
object ModProperties {
    @SubscribeEvent
    fun propertyOverrideRegistry(event: FMLClientSetupEvent) {
        event.enqueueWork {
            ItemProperties.register(ModItems.MONITOR.get(), loc("monitor_linked")) { itemStack, _, _, _ ->
                if (itemStack.tag != null && itemStack.tag!!.getBoolean("Linked")) 1f else 0f
            }
        }
        event.enqueueWork {
            ItemProperties.register(ModItems.ARMOR_PLATE.get(), loc("armor_plate_infinite")) { itemStack, _, _, _ ->
                if (itemStack.tag != null && itemStack.tag!!.getBoolean("Infinite")) 1f else 0f
            }
        }
    }
}