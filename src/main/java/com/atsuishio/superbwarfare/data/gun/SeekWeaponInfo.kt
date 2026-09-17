package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.StringOrVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SeekWeaponInfo(
    @SerialName("SeekDirection")
    val seekDirection: StringOrVec3 = StringOrVec3("Default"),

    @SerialName("SeekRange")
    val seekRange: Double = 384.0,

    @SerialName("SeekAngle")
    val seekAngle: Double = 20.0,

    @SerialName("MinTargetHeight")
    val minTargetHeight: Double = 0.0,

    @SerialName("MaxTargetHeight")
    val maxTargetHeight: Double = 114514.0,

    @SerialName("SeekTime")
    val seekTime: Int = 10,

    @SerialName("MinTargetSize")
    val minTargetSize: Double = 0.0,

    @SerialName("CalculateTrajectory")
    val calculateTrajectory: Boolean = false,

    @SerialName("OnlyLockBlock")
    val onlyLockBlock: Boolean = false,

    @SerialName("OnlyLockEntity")
    val onlyLockEntity: Boolean = false,

    @SerialName("InputBlockPos")
    val inputBlockPos: Boolean = false,

    @SerialName("MaxGuidedRange")
    val maxGuidedRange: Double = 2048.0,

    @SerialName("CanGuidedByRadar")
    val canGuidedByRadar: Boolean = true,

    @SerialName("AffectedByStealthTarget")
    val affectedByStealthTarget: Boolean = true,
)
