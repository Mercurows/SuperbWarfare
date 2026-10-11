package com.atsuishio.superbwarfare.resource.gun.pojo

import com.atsuishio.superbwarfare.serialization.kserializer.SerializedVector3f
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.joml.Vector3f

@Serializable
class ItemDisplayInfo {
    @JvmField
    @SerialName("translation")
    var translation: SerializedVector3f = Vector3f(0f, 0f, 0f)

    @JvmField
    @SerialName("rotation")
    var rotation: SerializedVector3f = Vector3f(0f, 0f, 0f)

    @JvmField
    @SerialName("scale")
    var scale: SerializedVector3f = Vector3f(0.65f, 0.65f, 0.65f)
}