package com.atsuishio.superbwarfare.data.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.math.max

@Serializable
enum class ChargeTrigger {
    @SerialName("AutoAtFull")
    AUTO_AT_FULL,

    @SerialName("OnRelease")
    ON_RELEASE,

    @SerialName("OnFullRelease")
    ON_FULL_RELEASE,
}

@Serializable
enum class ChargePowerMode {
    @SerialName("Full")
    FULL,

    @SerialName("Current")
    CURRENT,
}

/**
 * Data-driven charge configuration shared by HOLD and CHARGE fire modes.
 *
 * HOLD uses [ChargeTrigger.AUTO_AT_FULL]: early release cancels the charge and
 * reaching 100% fires once. CHARGE uses [ChargeTrigger.ON_RELEASE]: the charge
 * can be released at any progress. [ChargeTrigger.ON_FULL_RELEASE] is reserved
 * for weapons that may only release after reaching full charge.
 */
@Serializable
class ChargeInfo {
    @JvmField
    @SerialName("Duration")
    var duration: Int = 20

    @JvmField
    @SerialName("Trigger")
    var trigger: ChargeTrigger = ChargeTrigger.AUTO_AT_FULL

    @JvmField
    @SerialName("PowerMode")
    var powerMode: ChargePowerMode = ChargePowerMode.FULL

    @JvmField
    @SerialName("MinPower")
    var minPower: Double = 1.0

    init {
        duration = max(1, duration)
        minPower = minPower.coerceIn(0.0, 1.0)
    }

    fun powerForProgress(progress: Double): Double {
        return when (powerMode) {
            ChargePowerMode.FULL -> 1.0
            ChargePowerMode.CURRENT -> progress.coerceIn(0.0, 1.0)
        }
    }

    companion object {
        /**
         * Defaults for QL1031-style HOLD: wait until full, then fire once.
         */
        @JvmStatic
        fun holdDefaults(): ChargeInfo = ChargeInfo().apply {
            duration = 20
            trigger = ChargeTrigger.AUTO_AT_FULL
            powerMode = ChargePowerMode.FULL
            minPower = 1.0
        }

        /**
         * Defaults for Bocek-style CHARGE: release fires at the current power.
         */
        @JvmStatic
        fun chargeDefaults(): ChargeInfo = ChargeInfo().apply {
            duration = 20
            trigger = ChargeTrigger.ON_RELEASE
            powerMode = ChargePowerMode.CURRENT
            minPower = 0.0
        }
    }
}
