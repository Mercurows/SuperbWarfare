package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.atsuishio.superbwarfare.data.StringOrVec3
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RadarInfo {
    @SerializedName("Direction")
    @SerialName("Direction")
    var direction: StringOrVec3? = null

    @SerializedName("Range")
    @SerialName("Range")
    var range: Float = 128f

    @SerializedName("Angle")
    @SerialName("Angle")
    var angle: Float = 60f

    @SerializedName("RotateSpeed")
    @SerialName("RotateSpeed")
    var rotateSpeed: Float = 1f

    @SerializedName("ShareWithTeammates")
    @SerialName("ShareWithTeammates")
    var shareWithTeammates: Boolean = false

    @SerializedName("MaxTargetHeight")
    @SerialName("MaxTargetHeight")
    var maxTargetHeight: Double = 114514.0

    @SerializedName("MinTargetHeight")
    @SerialName("MinTargetHeight")
    var minTargetHeight: Double = -64.0

    @SerializedName("AffectedByStealthTarget")
    @SerialName("AffectedByStealthTarget")
    var affectedByStealthTarget = true
}
