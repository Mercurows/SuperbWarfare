package com.atsuishio.superbwarfare.compat.kubejs.event

import com.atsuishio.superbwarfare.api.event.LoadingDataEvent
import com.atsuishio.superbwarfare.api.event.LoadingJsonEvent
import dev.architectury.event.EventResult
import dev.latvian.mods.kubejs.event.EventGroup
import dev.latvian.mods.kubejs.event.EventHandler

object SbwKJSEventHandler {
    val GROUP: EventGroup = EventGroup.of("SuperbWarfareEvents")

    val LOADING_DATA_GUN: EventHandler = GROUP.server("loadingDataGun") { LoadingDataEventJS.Gun::class.java }
    val LOADING_DATA_VEHICLE: EventHandler =
        GROUP.server("loadingDataVehicle") { LoadingDataEventJS.Vehicle::class.java }
    val LOADING_JSON: EventHandler = GROUP.server("loadingJson") { LoadingJsonEventJS::class.java }

    fun init() {
        SbwKJSEventSignatures.LOADING_DATA_GUN.register(::onLoadingDataGun)
        SbwKJSEventSignatures.LOADING_DATA_VEHICLE.register(::onLoadingDataVehicle)
        SbwKJSEventSignatures.LOADING_JSON.register(::onLoadingJson)
    }

    private fun onLoadingDataGun(event: LoadingDataEvent.Gun): EventResult {
        return if (LOADING_DATA_GUN.hasListeners())
            LOADING_DATA_GUN.post(LoadingDataEventJS.Gun(event)).arch()
        else EventResult.pass()
    }

    private fun onLoadingDataVehicle(event: LoadingDataEvent.Vehicle): EventResult {
        return if (LOADING_DATA_VEHICLE.hasListeners())
            LOADING_DATA_VEHICLE.post(LoadingDataEventJS.Vehicle(event)).arch()
        else EventResult.pass()
    }

    private fun onLoadingJson(event: LoadingJsonEvent): EventResult {
        return if (LOADING_JSON.hasListeners())
            LOADING_JSON.post(LoadingJsonEventJS(event)).arch()
        else EventResult.pass()
    }
}