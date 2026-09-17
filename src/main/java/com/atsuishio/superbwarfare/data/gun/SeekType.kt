package com.atsuishio.superbwarfare.data.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SeekType {
    @SerialName("None")
    NONE,

    @SerialName("HoldFire")
    HOLD_FIRE,

    @SerialName("HoldZoom")
    HOLD_ZOOM,
}
