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

    @SerialName("ThirdPersonBone")
    var thirdPersonBone: String? = null

    // 第一人称（含载具瞄准）下是否允许按自由视角键（默认 C）自由环视
    @SerialName("HasFirstPersonFreeCamera")
    var hasFirstPersonFreeCamera: Boolean? = true

    // 第三人称下是否允许自由视角（自由环视 + 滚轮拉远/拉近）
    @SerialName("HasThirdPersonFreeCamera")
    var hasThirdPersonFreeCamera: Boolean? = true

    companion object {
        const val DIRECTION_BONE = "Bone"
    }
}
