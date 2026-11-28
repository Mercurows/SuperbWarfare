package com.atsuishio.superbwarfare.data.vehicle.subdata

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVec3
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.world.phys.Vec3

@Serializable
data class HandLoadedPayload(
    @SerialName("Weapon")
    val weapon: String,
    @SerialName("ReleaseOffset")
    val releaseOffset: SerializedVec3 = Vec3.ZERO,
)
