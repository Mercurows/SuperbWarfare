package com.atsuishio.superbwarfare.data.vehicle_skin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SkinInfo(
    @SerialName("Id")
    val id: String = "vanilla",
    @SerialName("Name")
    val name: String = "Vanilla",
    @SerialName("Description")
    val description: String = "",
    @SerialName("Texture")
    val texture: String = "",
    @SerialName("Priority")
    val priority: Int = 0
)

@Serializable
data class VehicleSkinData(
    @SerialName("Skins")
    val skins: List<SkinInfo> = listOf()
)
