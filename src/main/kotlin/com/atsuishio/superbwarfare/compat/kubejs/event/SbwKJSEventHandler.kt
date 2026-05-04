package com.atsuishio.superbwarfare.compat.kubejs.event

import com.atsuishio.superbwarfare.api.event.LoadingDataEvent
import com.atsuishio.superbwarfare.api.event.LoadingJsonEvent
import dev.latvian.mods.kubejs.event.EventGroup
import dev.latvian.mods.kubejs.event.EventHandler
import net.neoforged.bus.api.SubscribeEvent

object SbwKJSEventHandler {
    val GROUP: EventGroup = EventGroup.of("SuperbWarfareEvents")

    val LOADING_DATA_GUN: EventHandler = GROUP.server("loadingDataGun") { LoadingDataEventJS.Gun::class.java }
    val LOADING_DATA_VEHICLE: EventHandler =
        GROUP.server("loadingDataVehicle") { LoadingDataEventJS.Vehicle::class.java }
    val LOADING_JSON: EventHandler = GROUP.server("loadingJson") { LoadingJsonEventJS::class.java }

    @SubscribeEvent
    fun fireLoadingDataGunEvent(event: LoadingDataEvent.Gun) {
        if (LOADING_DATA_GUN.hasListeners()) {
            LOADING_DATA_GUN.post(LoadingDataEventJS.Gun(event)).applyCancel(event)
        }
    }

    @SubscribeEvent
    fun fireLoadingDataVehicleEvent(event: LoadingDataEvent.Vehicle) {
        if (LOADING_DATA_VEHICLE.hasListeners()) {
            LOADING_DATA_VEHICLE.post(LoadingDataEventJS.Vehicle(event)).applyCancel(event)
        }
    }

    @SubscribeEvent
    fun fireLoadingDataEvent(event: LoadingJsonEvent) {
        if (LOADING_JSON.hasListeners()) {
            LOADING_JSON.post(LoadingJsonEventJS(event)).applyCancel(event)
        }
    }
}