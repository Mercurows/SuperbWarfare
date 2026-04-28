package com.atsuishio.superbwarfare.data.vehicle.subdata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class VehicleContainerType(val row: Int, val col: Int, private val hasMenu: Boolean) {
    @SerialName("Empty")
    EMPTY(0, 0, false),

    @SerialName("One")
    ONE(1, 1, false),

    @SerialName("Mini")
    MINI(1, 9, true),

    @SerialName("Small")
    SMALL(3, 9, true),

    @SerialName("Medium")
    MEDIUM(6, 9, true),

    @SerialName("Large")
    LARGE(6, 13, true),

    @SerialName("Huge")
    HUGE(6, 17, true),

    @SerialName("Special")
    SPECIAL(3, 4, false);

    fun hasMenu(): Boolean {
        return hasMenu
    }

    val size: Int
        get() = row * col
}
