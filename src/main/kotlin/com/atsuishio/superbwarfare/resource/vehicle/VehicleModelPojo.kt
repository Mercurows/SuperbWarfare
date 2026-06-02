package com.atsuishio.superbwarfare.resource.vehicle

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class VehicleModelPojo {
    @JvmField
    @SerialName("Model")
    var model: SerializedResourceLocation? = null

    @JvmField
    @SerialName("Texture")
    var texture: SerializedResourceLocation? = null

    @JvmField
    @SerialName("EmissiveTexture")
    var emissiveTexture: SerializedResourceLocation? = null

    @JvmField
    @SerialName("LODDistance")
    var distance: Int = 0

    fun isLOD() = this.distance > 0
}