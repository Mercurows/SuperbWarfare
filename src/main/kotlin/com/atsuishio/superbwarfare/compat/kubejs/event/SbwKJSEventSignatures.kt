package com.atsuishio.superbwarfare.compat.kubejs.event

import com.atsuishio.superbwarfare.api.event.LoadingDataEvent
import com.atsuishio.superbwarfare.api.event.LoadingJsonEvent
import dev.architectury.event.Event
import dev.architectury.event.EventFactory
import dev.architectury.event.EventResult

interface SbwKJSEventSignatures {
    companion object {
        val LOADING_DATA_GUN: Event<LoadingDataGun> = EventFactory.createEventResult(*arrayOfNulls<LoadingDataGun>(0))
        val LOADING_DATA_VEHICLE: Event<LoadingDataVehicle> =
            EventFactory.createEventResult(*arrayOfNulls<LoadingDataVehicle>(0))
        val LOADING_JSON: Event<LoadingJson> = EventFactory.createEventResult(*arrayOfNulls<LoadingJson>(0))
    }

    fun interface LoadingDataGun {
        fun loading(event: LoadingDataEvent.Gun): EventResult
    }

    fun interface LoadingDataVehicle {
        fun loading(event: LoadingDataEvent.Vehicle): EventResult
    }

    fun interface LoadingJson {
        fun loading(event: LoadingJsonEvent): EventResult
    }
}