package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.StringOrVec3
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SeekWeaponInfo {
    @SerializedName("SeekDirection")
    @SerialName("SeekDirection")
    var seekDirection: StringOrVec3 = StringOrVec3("Default")

    @SerializedName("SeekRange")
    @SerialName("SeekRange")
    var seekRange = 384.0

    @SerializedName("SeekAngle")
    @SerialName("SeekAngle")
    var seekAngle = 20.0

    @SerializedName("MinTargetHeight")
    @SerialName("MinTargetHeight")
    var minTargetHeight = 0.0

    @SerializedName("MaxTargetHeight")
    @SerialName("MaxTargetHeight")
    var maxTargetHeight = 114514.0

    @SerializedName("SeekTime")
    @SerialName("SeekTime")
    var seekTime = 10

    @SerializedName("MinTargetSize")
    @SerialName("MinTargetSize")
    var minTargetSize = 0.0

    @SerializedName("CalculateTrajectory")
    @SerialName("CalculateTrajectory")
    var calculateTrajectory = false

    @SerializedName("OnlyLockBlock")
    @SerialName("OnlyLockBlock")
    var onlyLockBlock = false

    @SerializedName("OnlyLockEntity")
    @SerialName("OnlyLockEntity")
    var onlyLockEntity = false

    @SerializedName("InputBlockPos")
    @SerialName("InputBlockPos")
    var inputBlockPos = false

    @SerializedName("MaxGuidedRange")
    @SerialName("MaxGuidedRange")
    var maxGuidedRange = 2048.0

    @SerializedName("CanGuidedByRadar")
    @SerialName("CanGuidedByRadar")
    var canGuidedByRadar = true

    @SerializedName("AffectedByStealthTarget")
    @SerialName("AffectedByStealthTarget")
    var affectedByStealthTarget = true
}
