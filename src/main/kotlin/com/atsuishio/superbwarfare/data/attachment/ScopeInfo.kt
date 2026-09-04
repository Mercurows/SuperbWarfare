package com.atsuishio.superbwarfare.data.attachment

import com.atsuishio.superbwarfare.data.ModColor
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScopeInfo(
    @SerialName("IsScope")
    val isScope: Boolean = false,

    @SerialName("IsSight")
    val isSight: Boolean = false,

    @SerialName("ScopeViewRadiusModifier")
    val scopeViewRadiusModifier: Float = 1f,

    @SerialName("CrosshairIcon")
    val crosshairIcon: SerializedResourceLocation? = null,

    @SerialName("CrosshairColor")
    val crosshairColor: ModColor? = null,
)
