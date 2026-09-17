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
    fun getDamageRate(): Double {
        return if (this.type == null) this.rate else this.type.rate
    }

    @Serializable
    enum class ReduceType(val typeName: String, val rate: Double, val minDistance: Double) {
        @SerialName("Shotgun")
        SHOTGUN("Shotgun", 0.05, 15.0),

        @SerialName("Sniper")
        SNIPER("Sniper", 0.001, 150.0),

        @SerialName("Heavy")
        HEAVY("Heavy", 0.0007, 250.0),

        @SerialName("Handgun")
        HANDGUN("Handgun", 0.03, 40.0),

        @SerialName("Rifle")
        RIFLE("Rifle", 0.007, 100.0),

        @SerialName("Smg")
        SMG("Smg", 0.02, 50.0),

        @SerialName("Empty")
        EMPTY("Empty", 0.0, 0.0),
        ;

        override fun toString() = this.typeName
    }
}
