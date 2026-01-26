package com.atsuishio.superbwarfare.compat

import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
object CompatHolder {
    const val DMV: String = "dreamaticvoyage"
    const val VRC: String = "virtuarealcraft"
    const val CLOTH_CONFIG: String = "cloth_config"
    const val COLD_SWEAT: String = "cold_sweat"
    const val REALCAMERA: String = "realcamera"
    const val NET_MUSIC: String = "netmusic"

    @SubscribeEvent
    fun onInterModEnqueue(event: InterModEnqueueEvent) {
        event.enqueueWork {
            hasMod(CLOTH_CONFIG) {
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT) { Runnable { ClothConfigHelper.registerScreen() } }
            }
        }
    }

    fun hasMod(modid: String, runnable: Runnable) {
        if (ModList.get().isLoaded(modid)) {
            runnable.run()
        }
    }
}