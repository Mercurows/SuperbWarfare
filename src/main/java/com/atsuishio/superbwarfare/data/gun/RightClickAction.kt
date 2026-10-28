package com.atsuishio.superbwarfare.data.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class RightClickAction {
    @SerialName("Aim")
    AIM,

    @SerialName("HeavyStrike")
    HEAVY_STRIKE,

    /** 按住抬刀保持，松开打出重击 */
    @SerialName("HeavyHold")
    HEAVY_HOLD,

    @SerialName("BayonetCharge")
    BAYONET_CHARGE,
}
