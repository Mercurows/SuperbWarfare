package com.atsuishio.superbwarfare.data.gun

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SeekType {
    @SerializedName("None")
    @SerialName("None")
    NONE,

    @SerializedName("HoldFire")
    @SerialName("HoldFire")
    HOLD_FIRE,

    @SerializedName("HoldZoom")
    @SerialName("HoldZoom")
    HOLD_ZOOM,
}
