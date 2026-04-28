package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.atsuishio.superbwarfare.data.StringOrVec3
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class DismountInfo {
    @SerialName("Transform")
    var transform: String = "Default"

    @SerialName("Position")
    var position: SerializedVec3? = null

    // 能否弹射成员
    @SerialName("CanEject")
    var canEject: Boolean = false

    @SerialName("EjectPosition")
    var ejectPosition: SerializedVec3? = null

    @SerialName("EjectDirection")
    var ejectDirection: StringOrVec3? = StringOrVec3("Up")

    @SerialName("EjectForce")
    var ejectForce: Double = 2.0
}
