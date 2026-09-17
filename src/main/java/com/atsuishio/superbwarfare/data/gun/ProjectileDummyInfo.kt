package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@Serializable
data class ProjectileDummyInfo(
    @SerialName("Offset")
    val offset: SerializedVec3 = Vec3.ZERO,

    @SerialName("Rotate")
    val rotate: SerializedVec3 = Vec3.ZERO,

    @SerialName("Scale")
    val scale: SerializedVec3 = Vec3(1.0, 1.0, 1.0),

    @SerialName("HideDummyWhileZooming")
    val hideDummyWhileZooming: Boolean = false,
)
