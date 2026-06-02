package com.atsuishio.superbwarfare.data.vehicle.subdata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class VehicleType {
    @SerialName("Empty")
    EMPTY,

    @SerialName("Tank")
    TANK,

    @SerialName("APC")
    APC,

    @SerialName("AA")
    AA,

    @SerialName("Airplane")
    AIRPLANE,

    @SerialName("Helicopter")
    HELICOPTER,

    @SerialName("Car")
    CAR,

    @SerialName("Artillery")
    ARTILLERY,

    @SerialName("Defense")
    DEFENSE,

    @SerialName("Boat")
    BOAT,

    @SerialName("Drone")
    DRONE,

    @SerialName("Special")
    SPECIAL,

    @SerialName("AirShip")
    AIRSHIP
}
