package com.atsuishio.superbwarfare.data.gun

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ReloadType {
    @SerializedName("Magazine")
    @SerialName("Magazine")
    MAGAZINE,

    @SerializedName("Clip")
    @SerialName("Clip")
    CLIP,

    @SerializedName("Iterative")
    @SerialName("Iterative")
    ITERATIVE,
}
