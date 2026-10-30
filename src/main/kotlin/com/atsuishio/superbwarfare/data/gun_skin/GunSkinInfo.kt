package com.atsuishio.superbwarfare.data.gun_skin

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedResourceLocation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Texture-only skin; the weapon retains its model, animations and properties. */
@Serializable
data class GunSkinInfo(
    @SerialName("Id") val id: String = "",
    @SerialName("Name") val name: String = "",
    @SerialName("Description") val description: String = "",
    @SerialName("Texture") val texture: SerializedResourceLocation? = null,
    /** Must match the UVs of the weapon's LOD model. Absent means render the full model. */
    @SerialName("LODTexture") val lodTexture: SerializedResourceLocation? = null,
    @SerialName("Priority") val priority: Int = 0,
)

@Serializable
data class GunSkinData(
    @SerialName("Skins") val skins: List<GunSkinInfo> = emptyList(),
)
