package com.atsuishio.superbwarfare.resource.gun.pojo

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Blade endpoints are offsets from [bone]'s pivot in Blockbench units. */
@Serializable
class BladeTrailInfo(
    @SerialName("Bone") val bone: String,
    @SerialName("Base") val base: SerializedVector3f,
    @SerialName("Tip") val tip: SerializedVector3f,
    @SerialName("Windows") val windows: Map<String, Window>,
) {
    /** Times in seconds on the source animation clip. */
    @Serializable
    class Window(
        @SerialName("Start") val start: Float,
        @SerialName("End") val end: Float,
    )
}
