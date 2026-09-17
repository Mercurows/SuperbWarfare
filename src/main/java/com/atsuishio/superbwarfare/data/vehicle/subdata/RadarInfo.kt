package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.atsuishio.superbwarfare.data.StringOrVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class RadarInfo {
    @SerialName("Direction")
    var direction: StringOrVec3? = null

    @SerialName("Range")
    var range: Float = 128f

    @SerialName("Angle")
    var angle: Float = 60f

    @SerialName("RotateSpeed")
    var rotateSpeed: Float = 1f

    @SerialName("ShareWithTeammates")
    var shareWithTeammates: Boolean = false

    @SerialName("MaxTargetHeight")
    var maxTargetHeight: Double = 114514.0

    @SerialName("MinTargetHeight")
    var minTargetHeight: Double = -64.0

    @SerialName("AffectedByStealthTarget")
    var affectedByStealthTarget = true
}
