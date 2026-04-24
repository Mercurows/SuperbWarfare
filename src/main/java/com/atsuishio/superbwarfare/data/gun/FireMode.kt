package com.atsuishio.superbwarfare.data.gun

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FireMode(name: String) {
    @SerializedName("Semi")
    @SerialName("Semi")
    SEMI("Semi"),

    @SerializedName("Burst")
    @SerialName("Burst")
    BURST("Burst"),

    @SerializedName("Auto")
    @SerialName("Auto")
    AUTO("Auto");

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
