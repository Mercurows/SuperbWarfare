package com.atsuishio.superbwarfare.data.vehicle.subdata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class EngineType {
    @SerialName("Empty")
    EMPTY,

    @SerialName("Fixed")
    FIXED,

    @SerialName("Wheel")
    WHEEL,

    @SerialName("Track")
    TRACK,

    @SerialName("Helicopter")
    HELICOPTER,

    @SerialName("Ship")
    SHIP,

    @SerialName("Aircraft")
    AIRCRAFT,

    @SerialName("WheelChair")
    WHEELCHAIR,

    @SerialName("Tom6")
    TOM6,

    @SerialName("AirShip")
    AIRSHIP
}
