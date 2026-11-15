package com.atsuishio.superbwarfare.data.vehicle.subdata

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
class CameraBone {

    @SerialName("Bone")
    var bone: String = ""

    @SerialName("ZoomBone")
    var zoomBone: String? = null

    @SerialName("Direction")
    var direction: String = DIRECTION_BONE

    @SerialName("ZoomDirection")
    var zoomDirection: String? = null

    companion object {
        const val DIRECTION_BONE = "Bone"
    }
}
