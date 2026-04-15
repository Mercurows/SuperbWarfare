package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.data.DeserializeFromString
import com.atsuishio.superbwarfare.data.JsonPropertyModifier
import com.atsuishio.superbwarfare.data.PMC
import com.atsuishio.superbwarfare.data.PropertyModifier1
import com.atsuishio.superbwarfare.serialization.kserializer.SerializedGsonObject
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class FireModeInfo : DeserializeFromString, GunPropertyModifier, PropertyModifier1<GunData, DefaultGunData> {
    @JvmField
    @SerializedName("Mode")
    @SerialName("Mode")
    var mode: FireMode? = FireMode.SEMI

    @JvmField
    @SerializedName("Name")
    @SerialName("Name")
    var name: String = "Semi"

    @SerializedName("Override")
    @SerialName("Override")
    var override: SerializedGsonObject? = null

    @Transient
    @kotlinx.serialization.Transient
    private val jsonPropModifier = JsonPropertyModifier<GunData, DefaultGunData>()

    override fun computeProperties(gunData: GunData, rawData: DefaultGunData): DefaultGunData {
        jsonPropModifier.update(override)
        return jsonPropModifier.computeProperties(gunData, rawData)
    }

    override fun modifyProperty(modifier: PMC<GunData, DefaultGunData>) {
        // TODO Json PropertyModifier
    }

    fun init() {
    }

    override fun deserializeFromString(str: String) {
        init()

        this.mode = FireMode.tryParse(str)
        this.name = str
    }
}
