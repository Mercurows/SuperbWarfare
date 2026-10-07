package com.atsuishio.superbwarfare.data.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DamageReduce(
    @SerialName("Type")
    val type: ReduceType? = null,

    @SerialName("Rate")
    val rate: Double = 0.0,

    @SerialName("MinDistance")
    val minDistance: Double = 0.0,
) {
    /**
     * 每米（超出起始距离之后）的伤害衰减率。
     *
     * 写了 `Type` 时以类型预设为准，`Rate` 只对没写 `Type` 的自定义配置生效
     * （现在只有 `bocek` 这么用）；两者都没有就是 `0.0`，即不做距离衰减。
     */
    fun getDamageRate(): Double {
        return if (this.type == null) this.rate else this.type.rate
    }

    /** 距离衰减的起始距离（格），取值规则同 [getDamageRate]。 */
    fun getDamageMinDistance(): Double {
        return if (this.type == null) this.minDistance else this.type.minDistance
    }

    @Serializable
    enum class ReduceType(val typeName: String, val rate: Double, val minDistance: Double) {
        @SerialName("Shotgun")
        SHOTGUN("Shotgun", 0.05, 10.0),

        @SerialName("Sniper")
        SNIPER("Sniper", 0.0005, 100.0),

        @SerialName("Heavy")
        HEAVY("Heavy", 0.0003, 150.0),

        @SerialName("Handgun")
        HANDGUN("Handgun", 0.03, 20.0),

        @SerialName("Rifle")
        RIFLE("Rifle", 0.005, 50.0),

        @SerialName("Smg")
        SMG("Smg", 0.02, 20.0),

        @SerialName("Empty")
        EMPTY("Empty", 0.0, 0.0),
        ;

        override fun toString() = this.typeName
    }
}
