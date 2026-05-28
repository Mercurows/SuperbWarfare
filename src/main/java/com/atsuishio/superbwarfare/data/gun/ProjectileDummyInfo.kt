package com.atsuishio.superbwarfare.data.gun

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@Serializable
class ProjectileDummyInfo {

    @SerializedName("Offset")
    @SerialName("Offset")
    var offset: SerializedVec3 = Vec3.ZERO

    @SerializedName("Rotate")
    @SerialName("Rotate")
    var rotate: SerializedVec3 = Vec3.ZERO

    @SerializedName("Scale")
    @SerialName("Scale")
    var scale: SerializedVec3 = Vec3(1.0, 1.0, 1.0)

    @SerializedName("HideDummyWhileZooming")
    @SerialName("HideDummyWhileZooming")
    var hideDummyWhileZooming = false
}
