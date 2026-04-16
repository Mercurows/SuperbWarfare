package com.atsuishio.superbwarfare.api.event

import com.atsuishio.superbwarfare.data.gun.DefaultGunData
import com.atsuishio.superbwarfare.data.vehicle.DefaultVehicleData
import net.minecraftforge.eventbus.api.Cancelable
import net.minecraftforge.eventbus.api.Event
import org.jetbrains.annotations.ApiStatus

@Cancelable
@ApiStatus.AvailableSince("0.8.9")
open class LoadingDataEvent<T : Any> private constructor(
    val id: String,
    var data: T
) : Event() {
    class Gun(id: String, data: DefaultGunData) : LoadingDataEvent<DefaultGunData>(id, data)

    class Vehicle(id: String, data: DefaultVehicleData) : LoadingDataEvent<DefaultVehicleData>(id, data)
}