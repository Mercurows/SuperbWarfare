package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.*
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedGsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@STOFactory(FireModeInfo.FireModeInfoInstanceBuilder::class)
@Serializable
class FireModeInfo : DeserializeFromString, PropertyModifier<GunData, DefaultGunData> {
    @JvmField
    @SerializedName("Mode")
    @SerialName("Mode")
    var mode: FireMode? = FireMode.SEMI

    @JvmField
    @SerializedName("Name")
    @SerialName("Name")
    var name: String = "Semi"

    @JvmField
    @SerialName("Charge")
    var charge: ChargeInfo? = null

    @SerializedName("Override")
    @SerialName("Override")
    var override: SerializedGsonObject? = null

    @Transient
    @kotlinx.serialization.Transient
    private val jsonPropModifier = JsonPropertyModifier(GunProp.entries)

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        jsonPropModifier.update(override)
        jsonPropModifier.modifyProperty(modifier)
    }

    fun init() {
        if (charge == null) {
            charge = when (mode) {
                FireMode.HOLD -> ChargeInfo.holdDefaults()
                FireMode.CHARGE -> ChargeInfo.chargeDefaults()
                else -> null
            }
        }
    }

    fun isChargeMode(): Boolean {
        return mode == FireMode.HOLD || mode == FireMode.CHARGE
    }

    fun chargeConfig(): ChargeInfo? {
        return when (mode) {
            FireMode.HOLD -> charge ?: ChargeInfo.holdDefaults()
            FireMode.CHARGE -> charge ?: ChargeInfo.chargeDefaults()
            else -> null
        }
    }

    override fun deserializeFromString(str: String) {
        this.mode = FireMode.tryParse(str)
        this.name = str
        init()
    }

    object FireModeInfoInstanceBuilder : StringInstanceBuilder<FireModeInfo> {
        override fun fromString(value: String) = FireModeInfo().apply {
            this.mode = FireMode.tryParse(value)
            this.name = value
            init()
        }
    }
}
