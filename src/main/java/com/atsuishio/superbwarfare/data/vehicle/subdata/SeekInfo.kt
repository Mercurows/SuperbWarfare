package com.atsuishio.superbwarfare.data.vehicle.subdata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class SeekInfo {
    @SerialName("MaxSeekRange")
    var maxSeekRange: Double = 64.0

    @SerialName("MinSeekRange")
    var minSeekRange: Double = 1.0

    @SerialName("ChangeTargetTime")
    var changeTargetTime: Int = 60

    @SerialName("SeekIterative")
    var seekIterative: Int = 20

    @SerialName("MinTargetSize")
    var minTargetSize: Double = 0.25

    @SerialName("SeekEnergyCost")
    var seekEnergyCost: Int = 1000
}
