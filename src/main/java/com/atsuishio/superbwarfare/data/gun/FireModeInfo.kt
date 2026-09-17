package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@StringOrObjectFactory(FireModeInfo.FireModeInfoInstanceBuilder::class)
@Serializable
data class FireModeInfo(
    @JvmField
    @SerialName("Mode")
    val mode: FireMode? = FireMode.SEMI,

    @JvmField
    @SerialName("Name")
    val name: String = "Semi",

    @JvmField
    @SerialName("Charge")
    val charge: ChargeInfo? = null,

    @SerialName("Override")
    val override: JsonObject? = null,
) : PropertyModifier<GunData, DefaultGunData> {
    @Transient
    @kotlinx.serialization.Transient
    private val jsonPropModifier = JsonOverrideApplier(GunProp.entries)

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        jsonPropModifier.update(override)
        jsonPropModifier.modifyProperty(modifier)
    }

    fun isChargeMode(): Boolean {
        return mode == FireMode.HOLD || mode == FireMode.CHARGE
    }

    /**
     * 该开火模式实际使用的蓄力配置。
     *
     * 原实现在 `init()` 里把默认值写回 [charge] 字段；改成不可变之后直接在这里兜底，
     * 所有调用方本来就只通过本方法读取（`charge` 字段保持 JSON 里的原值）。
     */
    fun chargeConfig(): ChargeInfo? {
        return when (mode) {
            FireMode.HOLD -> charge ?: ChargeInfo.holdDefaults()
            FireMode.CHARGE -> charge ?: ChargeInfo.chargeDefaults()
            else -> null
        }
    }

    object FireModeInfoInstanceBuilder : StringInstanceBuilder<FireModeInfo> {
        override fun fromString(value: String) = FireModeInfo(
            mode = FireMode.tryParse(value),
            name = value,
        )
    }
}
