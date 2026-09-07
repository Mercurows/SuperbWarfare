package com.atsuishio.superbwarfare.data.gun

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FireMode(name: String) {
    @SerialName("Semi")
    SEMI("Semi"),

    @SerialName("Burst")
    BURST("Burst"),

    @SerialName("Auto")
    AUTO("Auto"),

    @SerialName("Hold")
    HOLD("Hold"),

    @SerialName("Charge")
    CHARGE("Charge");

    val typeName: String = name

    override fun toString(): String {
        return this.typeName
    }

    companion object {
        fun tryParse(value: String?): FireMode {
            for (enumConstant in FireMode.entries) {
                if (enumConstant.toString() == value) {
                    return enumConstant
                }
            }
            return SEMI
        }
    }
}
