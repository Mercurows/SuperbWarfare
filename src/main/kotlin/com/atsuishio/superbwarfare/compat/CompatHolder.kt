package com.atsuishio.superbwarfare.compat

import com.atsuishio.superbwarfare.Mod
import com.atsuishio.superbwarfare.compat.clothconfig.ClothConfigHelper
import net.neoforged.api.distmarker.Dist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.event.lifecycle.InterModEnqueueEvent
import net.neoforged.fml.loading.FMLEnvironment

@EventBusSubscriber(modid = Mod.MODID)
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
                if (FMLEnvironment.dist == Dist.CLIENT) {
                    ClothConfigHelper.registerScreen()
                }
            }
        }
    }

    fun hasMod(modid: String, runnable: Runnable) {
        if (ModList.get().isLoaded(modid)) {
            runnable.run()
        }
    }
}
