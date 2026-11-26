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

    /** 第三人称（非推镜）位置骨骼：用于复刻旧 UseAircraftCamera 的环视/滚轮拉远，roll 保持水平 */
    @SerialName("ThirdPersonBone")
    var thirdPersonBone: String? = null

    companion object {
        const val DIRECTION_BONE = "Bone"
    }
}
