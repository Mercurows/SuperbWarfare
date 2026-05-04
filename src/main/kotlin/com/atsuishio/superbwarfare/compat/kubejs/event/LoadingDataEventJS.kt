package com.atsuishio.superbwarfare.compat.kubejs.event

import com.atsuishio.superbwarfare.api.event.LoadingDataEvent
import dev.latvian.mods.kubejs.event.KubeEvent

abstract class LoadingDataEventJS : KubeEvent {
    class Gun(val event: LoadingDataEvent.Gun) : LoadingDataEventJS() {
        var id = event.id
        var data = event.data
    }

    class Vehicle(val event: LoadingDataEvent.Vehicle) : LoadingDataEventJS() {
        var id = event.id
        var data = event.data
    }
}