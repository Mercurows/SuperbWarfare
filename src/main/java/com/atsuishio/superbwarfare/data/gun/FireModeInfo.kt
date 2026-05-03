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
    }

    override fun deserializeFromString(str: String) {
        init()

        this.mode = FireMode.tryParse(str)
        this.name = str
    }

    object FireModeInfoInstanceBuilder : StringInstanceBuilder<FireModeInfo> {
        override fun fromString(value: String) = FireModeInfo().apply {
            init()
            this.mode = FireMode.tryParse(value)
            this.name = value
        }
    }
}
