package com.atsuishio.superbwarfare.resource.gun.pojo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class MuzzleFxInfo {
    @SerialName("Trigger") val trigger: String = "animation"
    @SerialName("Flash") val flash: Flash = Flash()
    @SerialName("Smoke") val smoke: Smoke = Smoke()
    @SerialName("Sparks") val sparks: Sparks = Sparks()

    @Serializable class Flash {
        @SerialName("Enabled") val enabled: Boolean = true
        @SerialName("Duration") val duration: Float = 0.045f
        @SerialName("Variants") val variants: Int = 8
        @SerialName("Length") val length: Float = 0.85f
    }

    @Serializable class Smoke {
        @SerialName("Enabled") val enabled: Boolean? = null
        @SerialName("NearCount") val nearCount: Int? = null
        @SerialName("TailCount") val tailCount: Int = 0
    }

    @Serializable class Sparks {
        @SerialName("Probability") val probability: Float = 0.12f
        @SerialName("MaxCount") val maxCount: Int = 2
    }
}
