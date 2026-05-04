package com.atsuishio.superbwarfare.compat.kubejs

import com.atsuishio.superbwarfare.api.event.LoadingDataEvent
import com.atsuishio.superbwarfare.api.event.LoadingJsonEvent
import com.atsuishio.superbwarfare.compat.kubejs.event.SbwKJSEventHandler
import com.atsuishio.superbwarfare.compat.kubejs.event.SbwKJSEventSignatures
import dev.latvian.mods.kubejs.KubeJSPlugin
import net.minecraftforge.eventbus.api.SubscribeEvent
import thedarkcolour.kotlinforforge.forge.FORGE_BUS

class SbwKubeJSPlugin : KubeJSPlugin() {
    override fun registerEvents() {
        SbwKJSEventHandler.GROUP.register()
        SbwKJSEventHandler.init()
        FORGE_BUS.register(this)
    }

    @SubscribeEvent
    fun fireLoadingDataGunEvent(event: LoadingDataEvent.Gun) {
        val res = SbwKJSEventSignatures.LOADING_DATA_GUN.invoker().loading(event)
        if (res.isFalse) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun fireLoadingDataVehicleEvent(event: LoadingDataEvent.Vehicle) {
        val res = SbwKJSEventSignatures.LOADING_DATA_VEHICLE.invoker().loading(event)
        if (res.isFalse) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun fireLoadingDataEvent(event: LoadingJsonEvent) {
        val res = SbwKJSEventSignatures.LOADING_JSON.invoker().loading(event)
        if (res.isFalse) {
            event.isCanceled = true
        }
    }
}